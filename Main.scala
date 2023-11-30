//> using toolkit latest
//> using dep redis.clients:jedis:5.1.0
//> using dep com.dinstone.beanstalkc:beanstalkc-netty:2.4.0

import com.dinstone.beanstalkc.BeanstalkClientFactory
import com.dinstone.beanstalkc.Configuration
import com.dinstone.beanstalkc.JobConsumer
import com.dinstone.beanstalkc.JobProducer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

@main
def main() =
  val queueType          = sys.env.getOrElse("QUEUE_TYPE", "redis")
  val hostname           = sys.env.getOrElse("HOSTNAME", "localhost")
  val port               = sys.env.getOrElse("PORT", "6379").toInt
  val numberOfOperations = sys.env.getOrElse("NUMBER_OF_OPERATIONS", "1000000").toInt
  benchmarkQueueOperations(queueType, hostname, port, numberOfOperations)

def benchmarkQueueOperations(queueType: String, hostname: String, port: Int, numberOfOperations: Int) =
  lazy val redisW      = new RedisW(hostname, port)
  lazy val beanstalkdW = new BeanstalkdW(hostname, port)

  def op() = queueType.toLowerCase match
    case "redis" =>
      val uniqueKey = s"key-${UUID.randomUUID()}"
      for {
        _ <- redisW.push(uniqueKey)
        _ <- redisW.pop(uniqueKey)
      } yield ()
    case "beanstalkd" =>
      for {
        _ <- beanstalkdW.push(s"key-${UUID.randomUUID()}")
        _ <- beanstalkdW.pop()
      } yield ()

  val startTime = System.currentTimeMillis()

  val futures = for { _ <- 1 to numberOfOperations } yield Future { op().fold(error => println(error), success => ()) }
  Await.result(Future.sequence(futures), Duration.Inf)

  val endTime             = System.currentTimeMillis()
  val durationInSeconds   = (endTime - startTime) / 1000.0
  val operationsPerSecond = numberOfOperations / durationInSeconds

  println(s"Total duration        : ${durationInSeconds}s")
  println(s"Total number of ops   : ${numberOfOperations}")
  println(s"Operations per second : ${operationsPerSecond}/s")

  beanstalkdW.close()

class RedisW(hostname: String, port: Int):
  private val pool = new JedisPool(hostname, port);

  private def withJedis[T](f: Jedis => T): Try[T] =
    val jedis = pool.getResource()
    val res   = Try(f(jedis))
    jedis.close()
    res

  private def write(key: String, value: String) = withJedis(_.set(key, value))
  private def read(key: String)                 = withJedis(j => Option(j.get(key)))
  private def delete(key: String)               = withJedis(_.del(key))

  def push(key: String): Try[Unit] = write(key, s"value-$key").map(_ => ())
  def pop(key: String): Try[Option[String]] = for {
    msgOpt <- read(key)
    result <- msgOpt match
      case Some(msg) => delete(key).map(_ => Some(msg))
      case None      => Try(None)
  } yield result

class BeanstalkdW(hostname: String, port: Int):
  val factory = new BeanstalkClientFactory({
    val config = new Configuration()
    config.setServiceHost(hostname)
    config.setServicePort(port)
    config.setConnectTimeout(2000);
    config.setReadTimeout(3000);
    config
  })
  val producer = factory.createJobProducer("benchmark")
  val consumer = factory.createJobConsumer("benchmark")

  private def write(msg: String)  = Try(producer.putJob(0, 0, 5, msg.getBytes))
  private def read()              = Try(Some(consumer.reserveJob(0)).map(job => (job.getId(), String(job.getData()))))
  private def delete(jobId: Long) = Try(consumer.deleteJob(jobId))

  def push(key: String): Try[Long] = write(s"$key-value-$key")
  def pop(): Try[Option[String]] = for {
    jobOption <- read()
    result <- jobOption match
      case Some((jobId, msg)) => delete(jobId).map{_ => Some(msg)}
      case None               => Try(None)
  } yield result

  def close() = 
    producer.close()
    consumer.close()