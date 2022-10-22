package cn.navclub.nes4j.bin.test;

import cn.navclub.nes4j.bin.util.MathUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteTest {
    @Test
    void test_basic_add() {
        var a = 10;
        var b = 20;
        var m = MathUtil.addition(a, b, false);
        Assertions.assertEquals(m.result(), 30);
    }

    @Test
    void test_add_neg() {
        var a = 10;
        var b = -20;
        var m = MathUtil.addition(a, b, false);
        Assertions.assertEquals(m.result(), -10);
    }

    @Test
    void test_neg_add() {
        var a = -10;
        var b = 20;
        var m = MathUtil.addition(a, b, false);
        Assertions.assertEquals(m.result(), 10);
    }

    @Test
    void test_carry_add() {
        var a = 255;
        var b = 1;
        var m = MathUtil.addition(a, b, false);
        Assertions.assertEquals(m.result(), 0);
        Assertions.assertTrue(m.carry());
    }

    @Test
    void test_b16_add() {
        var al = 0x10;
        var am = 0x20;
        var bl = 0x10;
        var bm = 0x20;
        var rs = 0;
        var m = MathUtil.addition(al, bl, false);
        rs = m.result();
        m = MathUtil.addition(am, bm, m.carry());
        rs |= (m.result() << 8);
        Assertions.assertFalse(m.carry());
        Assertions.assertEquals(rs, 0x2010 * 2);
    }
}
