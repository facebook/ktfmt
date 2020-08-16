package com.facebook.ktfmt.intellij;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FormatterUtilTest {
  @Test
  public void getReplacements() throws Exception {
    String code = "val   a =    5";
    String expected = "val a = 5\n";
    String actual = FormatterUtil.formatCode(false, code);

    assertEquals(expected, actual);
  }
}
