package study.hank.com.myapplication;

public class Test {


    private void 普通位运算() {
        //二进制 位运算
        int a = 0b0;
        int b = 0b1;
        System.out.println(a & b);//按位与&(只有1&1才是1，其他的都是0)
        System.out.println(a | b);//按位或|(只有0|0才是0，其他的都是1)
    }

    /**
     * &的特殊用法1
     * <p>
     * 利用&的基本原则，除了1&1是1，其他的都是0
     */
    private static void 清零() {
        //清零操作
        int a1 = 0b10000;
        int a0 = 0b00100;
        printBinaryString(a1 | a0);
    }

    /**
     * &的特殊用法2
     */
    private static void 取一个数的指定位() {
        int a = 0b111101;
        int b = 0b001111;//取后4位
        printBinaryString(a & b);
    }

    private static void 普通或运算() {
        int a = 0b110011;
        int b = 0b101;
        printBinaryString(a | b);
    }

    /**
     * 给某些位  置为1(用1去|，就变成了1)
     */
    private static void 或运算特殊用法() {
        int a = 0b110011;
        int b = 0b000100;
        printBinaryString(a | b);
    }

    /**
     * ^符号，表示异或计算
     * <p>
     * 相同，则0，不同，则1
     */
    private static void 异或() {
        System.out.print("异或:1 ^ 0=");
        printBinaryString(1 ^ 0);// 值不同，则结果位1，值相同，则结果位0，和&有区别
        System.out.print("异或:1 ^ 1=");
        printBinaryString(1 ^ 1);// 值不同，则结果位1，值相同，则结果位0，和&有区别
        System.out.print("异或:0 ^ 0=");
        printBinaryString(0 ^ 0);// 值不同，则结果位1，值相同，则结果位0，和&有区别
    }

    /**
     * 指定位置翻转,用111来和他进行^运算，可以让指定位置的值反过来
     * <p>
     * 利用原理 ^ 相同则0，不同则1
     */
    private static void 异或特殊用法() {
        int a = 0b111101;
        int b = 0b010111;
        System.out.println("a = 0b111101");
        System.out.print("与0b010111异或，值翻转");
        printBinaryString(a ^ b);// 值不同，则结果位1，值相同，则结果位0，和&有区别
        System.out.print("与0异或，值不变");
        printBinaryString(a ^ 0);// 值不同，则结果位1，值相同，则结果位0，和&有区别

        System.out.println("两个变量值的交换(这是效率极高的算法,直接利用异或运算)");

        a = 0b0;
        b = 0b1;
        a = a ^ b;// a 变成 1
        b = a ^ b;// b 变成 0
        a = a ^ b;// a 变成 1
        printBinaryString(a);
        printBinaryString(b);
    }

    private static void 取反与运算() {
        int a = 0b1;
        printBinaryString(~a);// 取反  所有位，全部取反（0变1，1变0）
    }

    /**
     * 所有位，全部左移，超出的部分丢弃，右边的部分补0
     */
    private static void 正数左移运算() {
        int a = 0b1100;//int一共32位，<<是左移运算  正数左移 所有位全部左移，超出部分丢弃，确实部分补0
        printBinaryString(a << 2);
    }

    /**
     * 所有位，全部左移，超出的部分丢弃，右边的部分补0
     */
    private static void 负数左移运算() {
        System.out.println("负数左移：");
        int a = -0b1100;//int一共32位，<<是左移运算  负数左移 所有位全部左移，超出部分丢弃，确实部分补0
        System.out.println(a << 2);//-48?尼玛？这个博客貌似写错了呀！？？？？
        printBinaryString(a << 1);//负数左移，所有位全部左移，右边补0，左边空白位全都补1
        printBinaryString(a << 2); //负数左移，左边全部补1

        //负数的左移，我怎么看不懂了？？！  负数的位移有点麻烦
    }

    private static void 正数右移运算() {
        int a = 0b1111100;//
        printBinaryString(a >> 2);// 正数的右移，所有位全部右移，超出部分丢弃，左边补0
    }


    private static void 负数右移运算() {
        System.out.println("负数右移：");
        int a = -0b1100;// 负数右移，有一个反码，补码的过程，贼特么复杂，计算的方式我还得研究一下

        //1110 是 十进制的14，不过上面是-14，那么，第一步，先取绝对值，1110
        //第二步，全部取反，全32位全部取反，由于上面的1110只是最后4位，其他位置全都是0，所以，取反之后，最后4位是，0001,其他位全都是1
        //于是得到 1111111111 1111111111 11111111 0001 （每一组分别是10+10+8+4位）,这是反码
        // 然后将上面的一大串反码+1，得到补码 得到 1111111111 1111111111 11111111 0010

        //现在来进行右移，所有位都右移，左边补1，右边多余的丢弃
        //于是得到 1111111111 1111111111 11111111 1100
        //平移完毕之后，这个还是补码，那么补码之前+1，现在-1，得到 1111111111 1111111111 11111111 1011
        //然后取反，得到0000000000 0000000000 00000000 0100 = 100（省略这么多0）

        //那么它的十进制值应该是 2^2*1+2^1*0+2^0*0 = 4

        //所以，-14 右移2位，得到的应该是-4


        //这个二进制的首位是1，说明是补码的形式，现在要将补码转化成原码，
        //
        System.out.println(a >> 2);//这里打印出来是-3，按照上面的算法，是-4，尼玛？
        printBinaryString(a >> 2);// 正数的右移，所有位全部右移，超出部分丢弃，左边补0
    }


    private static void printBinaryString(int i) {
        System.out.println(Integer.toBinaryString(i));
    }

    public static void main(String[] args) {
        int byteV = 0b1111;//二进制数,0b开头，每一位都是0或1
        int hexV = 0xabbc;//16进制 以0x开头，每一位都是 0-9, 或者abcdef
        int eV = 0100234567;//8进制？以0开头，每一位都是0-7
        int v = 123456789;//10进制
        //二进制，最大的作用就是 位运算
        //优点：
        清零();
        //原来如此，位运算的奥秘就是，对应位置上的数值，进行或与运算
        //位运算，原理，就是将二进制数字的对应位置进行 & | ^ 和其他运算
        取一个数的指定位();
        //
        普通或运算();
        或运算特殊用法();
        异或();
        异或特殊用法();
        取反与运算();
        正数左移运算();
        负数左移运算();
        正数右移运算();
        负数右移运算();
    }
}
