import org.apache.flink.util.MathUtils

/**
 * @Author lancer
 * @Date 2022/8/4 23:53
 * @Description
 */
object TestMainDemo {
  def main(args: Array[String]): Unit = {
    // -372811216
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))

    // 总能保证范围在127以内
    print(-372811216 & 127)

  }
}
