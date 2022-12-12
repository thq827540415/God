import com.solitude.basic.JavaGrammar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.lang.reflect.Field;
import java.util.*;


/**
 * @Author lancer
 * @Date 2022/1/6 7:07 下午
 * @Description
 */
public class MainTest {

    private static class Student implements Comparable<Student> {
        private String name;
        private Integer age;

        public Student(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "Student{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }

        @Override
        public int compareTo(Student o) {
            return this.getAge().compareTo(o.getAge());
        }
    }

    private static class AgeComparator implements Comparator<Student> {
        @Override
        public int compare(Student o1, Student o2) {
            return o2.getAge() - o1.getAge();
        }
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        // {"a":1,"b":{"c":2,"d":[3,4]}}
        // question02();
        /*Student[] students = new Student[] {
                new Student("zhangSan", 12),
                new Student("liSi", 15),
                new Student("wangWu", 11)
        };
        Arrays.sort(students);
        for (Student student : students) {
            System.out.println(student);
        }*/
    }

    /**
     * 利用反射，修改值
     */
    public static void question01() {
        String s = new String("abc");

        // 修改s的值，保证s的引用不变
        try {

            Field value = s.getClass().getDeclaredField("value");
            value.setAccessible(true);
            value.set(s, "abcd".toCharArray());

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        System.out.println(s);
    }

    /**
     * String问题
     */
    public static void question02() {
        // 两个对象，s1指向字符串对象
        String s1 = new String("abc");
        // 一个对象，s2指向字符串常量
        String s2 = "abc";
        // intern先检查字符串常量池中是否存在"abc"，存在则返回该字符串引用，否则，将"abc"添加到字符串常量池，再返回该字符串的引用
        String s3 = s1.intern();

        // false
        System.out.println(s1 == s2);

        // true
        System.out.println(s2 == s3);
    }

    /**
     * Integer问题
     */
    public static void question03() {
        Integer i1 = 100;
        Integer i2 = 100;
        System.out.println(i1 == i2);

        Integer i3 = 128;
        Integer i4 = 128;
        System.out.println(i3 == i4);

        Integer i5 = new Integer(100);
        System.out.println(i1 == i5);
    }

    /**
     * HashMap扩容机制
     * <p>
     * 1.7
     * 1.生成新数组
     * 2.遍历老数组中的每个位置上的链表上的每个元素
     * 3.取每个元素的key，并基于新数组长度，计算出每个元素在新数组中的下标
     * 4.将元素添加到新数组中去
     * 5.所有元素转移完之后，将新数组赋值给HashMap对象的table属性
     * <p>
     * 1.8
     * 1.生成新数组
     * 2.遍历老数组中的每个位置上的链表或红黑树
     * 3.如果是链表，则直接将链表中的每个元素重新计算下标，并添加到新数组中去
     * 4.如果是红黑树，则先遍历红黑树，先计算出红黑树中每个元素对应在新数组中的下标位置
     * a.统计每个下标位置的元素个数
     * b.如果该位置下的元素个数超过了8，则生成一个新的红黑树，并将根节点添加到新数组的对应位置
     * c.如果该位置下的元素个数没有超过8，则生成一个链表，并将链表的头节点添加到新数组的对应位置
     * 5.所有元素转移完之后，将新数组赋值给HashMap对象的table属性
     */
    public static void question04() {
        HashMap<String, String> hashMap = new HashMap<>();

    }

    /**
     *
     */
}
