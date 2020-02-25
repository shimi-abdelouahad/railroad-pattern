package com.as.lexer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("static-method")
public class LexerTest {
    @Tag("Q1") @Test
    public void testEmpty() {
        assertTrue(Lexer.create().tryParse("foo").isEmpty());
    }
    @Tag("Q1") @Test @SuppressWarnings("unused")
    public void testEmptyTyped() {
        Lexer<String> emptyString = Lexer.create();
        Lexer<Integer> emptyInteger = Lexer.create();
    }
    @Tag("Q1") @Test
    public void testEmptyInterned() {
        assertSame(Lexer.create(), Lexer.create());
    }
    @Tag("Q1") @Test @SuppressWarnings("unused")
    public void testEmptyTryParseTyped() {
        Optional<String> tokenString = Lexer.<String>create().tryParse("foo");
        Optional<Integer> tokenInteger = Lexer.<Integer>create().tryParse("123");
    }
    @Tag("Q1") @Test
    public void testEmptyTryParseNull() {
        assertThrows(NullPointerException.class, () -> Lexer.create().tryParse(null));
    }


  interface LexerFactory {
    Lexer<String> create(String regex);
  }
  @SuppressWarnings("unused")
  private static Stream<LexerFactory> lexerFactories() {
    return Stream.of(
        text -> Lexer.from(Pattern.compile(text)), Lexer::from //, Q6 text -> Lexer.from(List.of(text), List.of(x -> x))
        );
  }

  @Tag("Q2") @ParameterizedTest @MethodSource("lexerFactories")
  public void testFromPatternRecognized(LexerFactory factory) {
    var lexer = factory.create("(ab*)");
    assertAll(
        () -> assertEquals("a", lexer.tryParse("a").orElseThrow()),
        () -> assertEquals("ab", lexer.tryParse("ab").orElseThrow()),
        () -> assertEquals("abb", lexer.tryParse("abb").orElseThrow()),
        () -> assertEquals("abbb", lexer.tryParse("abbb").orElseThrow()),
        () -> assertEquals("abbbb", lexer.tryParse("abbbb").orElseThrow())
        );
  }
  @Tag("Q2") @ParameterizedTest @MethodSource("lexerFactories")
  public void testFromPatternRecognizedSubmatch(LexerFactory factory) {
    var lexer = factory.create("([0-9]+)\\.[0-9]*");
    assertAll(
        () -> assertEquals("12", lexer.tryParse("12.6").orElseThrow()),
        () -> assertEquals("67", lexer.tryParse("67.").orElseThrow()),
        () -> assertEquals("63", lexer.tryParse("63.5").orElseThrow()),
        () -> assertEquals("45", lexer.tryParse("45.9").orElseThrow()),
        () -> assertEquals("0", lexer.tryParse("0.0").orElseThrow())
        );
  }
  @Tag("Q2") @ParameterizedTest @MethodSource("lexerFactories")
  public void testFromPatternUnrecognized(LexerFactory factory) {
    var lexer = factory.create("(ab*)");
    assertAll(
        () -> assertTrue(lexer.tryParse("").isEmpty()),
        () -> assertTrue(lexer.tryParse("b").isEmpty()),
        () -> assertTrue(lexer.tryParse("foo").isEmpty()),
        () -> assertTrue(lexer.tryParse("bar").isEmpty()),
        () -> assertTrue(lexer.tryParse("ba").isEmpty())
        );
  }
  @Tag("Q2") @ParameterizedTest @MethodSource("lexerFactories")
  public void testFromOnlyOneCaptureGroup(LexerFactory factory) {
    assertAll(
      () -> assertThrows(IllegalArgumentException.class, () -> factory.create("foo")),
      () -> assertThrows(IllegalArgumentException.class, () -> factory.create("(foo)(bar)"))
      );
  }
  @Tag("Q2") @ParameterizedTest @MethodSource("lexerFactories")
  public void testFromTryParseNull(LexerFactory factory) {
    assertThrows(NullPointerException.class, () -> factory.create("(foo)").tryParse(null));
  }
  @Tag("Q2") @Test
  public void testFromNull() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> Lexer.from( (Pattern)null)),
        () -> assertThrows(NullPointerException.class, () -> Lexer.from((String)null))
        );
  }


  @Tag("Q3") @Test
  public void testMapRecognized() {
    assertAll(
      () -> assertEquals(42, (int)Lexer.from("([0-9]+)").map(Integer::parseInt).tryParse("42").orElseThrow()),
      () -> assertEquals(42.0, (double)Lexer.from("([0-9]+\\.[0-9]+)").map(Double::parseDouble).tryParse("42.0").orElseThrow())
      );
  }

  @Tag("Q3") @Test
  public void testMapUnrecognized() {
    assertAll(
      () -> assertTrue(Lexer.from("([0-9]+)").map(Integer::parseInt).tryParse("foo").isEmpty()),
      () -> assertTrue(Lexer.from("([0-9]+\\.[0-9]+)").map(Double::parseDouble).tryParse("bar").isEmpty()),
      () -> assertTrue(Lexer.<String>create().map(Integer::parseInt).tryParse("foo").isEmpty()),
      () -> assertTrue(Lexer.<String>create().map(Double::parseDouble).tryParse("bar").isEmpty())
      );
  }
  @Tag("Q3") @Test
  public void testMapNull() {
    assertThrows(NullPointerException.class, () -> Lexer.from("(f)oo").map(null));
  }
  @Tag("Q3") @Test
  public void testMapReturnNull() {
    assertTrue(Lexer.from("(foo)").map(__ -> null).tryParse("foo").isEmpty());
  }
  @Tag("Q3") @Test
  public void testMapSignature() {
    var lexer = Lexer.from("([0-9]+)").map(Integer::parseInt);
    assertEquals("1111", lexer.map((Object o) -> o.toString()).tryParse("1111").orElseThrow());
  }
  @Tag("Q3") @Test
  public void testMapSignature2() {
    Lexer<Object> lexer = Lexer.from("([0-9]+)").map(Integer::parseInt);
    assertEquals(747, (int)lexer.tryParse("747").orElseThrow());
  }


  @Tag("Q4") @Test
  public void testOr() {
    var lexer = Lexer.from("([0-9]+)").or(Lexer.from("([a-z_]+)"));
    assertAll(
        () -> assertEquals("17", lexer.tryParse("17").orElseThrow()),
        () -> assertEquals("foo", lexer.tryParse("foo").orElseThrow()),
        () -> assertTrue(lexer.tryParse("$bar").isEmpty())
        );
  }
  @Tag("Q4") @Test
  public void testOrEmpty() {
    var lexer = Lexer.create().or(Lexer.create());
    assertAll(
        () -> assertTrue(lexer.tryParse("42").isEmpty()),
        () -> assertTrue(lexer.tryParse("foo").isEmpty()),
        () -> assertTrue(lexer.tryParse("_bar_").isEmpty())
        );
  }
  @Tag("Q4") @Test
  public void testOrWithMapRecognized() {
    var lexer = Lexer.from("([0-9]+)").<Object>map(Integer::parseInt).or(Lexer.from("([a-z_]+)"));
    assertAll(
        () -> assertEquals(17, lexer.tryParse("17").orElseThrow()),
        () -> assertEquals("foo", lexer.tryParse("foo").orElseThrow())
        );
  }
  @Tag("Q4") @Test
  public void testOrChooseFirst() {
  var lexer = Lexer.from("(goto)").map(__ -> 0).or(Lexer.from("([a-z]+)").map(__ -> 1));
  assertAll(
      () -> assertEquals(0, (int)lexer.tryParse("goto").orElseThrow()),
      () -> assertEquals(1, (int)lexer.tryParse("foo").orElseThrow()),
      () -> assertTrue(lexer.tryParse("42").isEmpty())
      );
  }
  @Tag("Q4") @Test
  public void testOrNoHiddenSideEffect() {
    var lexer1 = Lexer.from("([a-z]+)").map(__ -> 777);
    var lexer2 = Lexer.from("([0-9]+)").map(Integer::parseInt);
    var lexer3 = lexer1.or(lexer2);
    assertAll(
      () -> assertTrue(lexer1.tryParse("17").isEmpty()),
      () -> assertTrue(lexer2.tryParse("aa").isEmpty()),
      () -> assertTrue(lexer3.tryParse("17").isPresent()),
      () -> assertTrue(lexer3.tryParse("aa").isPresent())
    );
  }
  @Tag("Q4") @Test
  public void testOrNull() {
    assertThrows(NullPointerException.class, () -> Lexer.from("(f)oo").or(null));
  }


  @Tag("Q5") @Test
  public void testWith() {
    var lexer = Lexer.<Integer>create().with("(9)X?X?", Integer::parseInt);
    assertAll(
        () -> assertEquals(9, (int)lexer.tryParse("9").orElseThrow()),
        () -> assertEquals(9, (int)lexer.tryParse("9X").orElseThrow()),
        () -> assertEquals(9, (int)lexer.tryParse("9XX").orElseThrow()),
        () -> assertTrue(lexer.tryParse("XXX").isEmpty())
        );
  }
  @Tag("Q5") @Test
  public void testSeveralWiths() {
    var lexer = Lexer.create()
        .with("(9)X?X?", Integer::parseInt)
        .with("(7)X?X?", Double::parseDouble);
    assertAll(
        () -> assertEquals(7.0, (double)lexer.tryParse("7").orElseThrow()),
        () -> assertEquals(9, (int)lexer.tryParse("9X").orElseThrow()),
        () -> assertEquals(7.0, (double)lexer.tryParse("7XX").orElseThrow()),
        () -> assertTrue(lexer.tryParse("XXX").isEmpty())
        );
  }
  @Tag("Q5") @Test
  public void testWithNoSideEffect() {
    var lexer1 = Lexer.create();
    var lexer2 = lexer1.with("(a*)b", String::length);
    var lexer3 = lexer2.with("(c*)d", String::length);
    assertAll(
      () -> assertTrue(lexer1.tryParse("ccd").isEmpty()),
      () -> assertTrue(lexer2.tryParse("ccd").isEmpty()),
      () -> assertEquals(2, (int)lexer3.tryParse("ccd").orElseThrow()),
      () -> assertEquals(3, (int)lexer3.tryParse("aaab").orElseThrow())
    );
  }
  @Tag("Q5") @Test
  public void testWithOneCaptureGroup() {
    assertAll(
      () -> assertThrows(IllegalArgumentException.class, () -> Lexer.create().with("bar", x -> x)),
      () -> assertThrows(IllegalArgumentException.class, () -> Lexer.create().with("(foo)(bar)", x -> x))
      );
  }
  @Tag("Q5") @Test
  public void testWithSomeNulls() {
    assertAll(
      () -> assertThrows(NullPointerException.class, () -> Lexer.create().with(null, x -> x)),
      () -> assertThrows(NullPointerException.class, () -> Lexer.create().with("(foo)", null))
      );
  }
  @Tag("Q5") @Test
  public void testCreate() {
    var lexer = Lexer.create()
        .with("([0-9]+)",          Integer::parseInt)
        .with("([0-9]+\\.[0-9]*)", Double::parseDouble)
        .with("([a-zA-Z]+)",       Function.identity());
    assertAll(
        () -> assertEquals("foo", lexer.tryParse("foo").orElseThrow()),
        () -> assertEquals(12.3, lexer.tryParse("12.3").orElseThrow()),
        () -> assertEquals(200, lexer.tryParse("200").orElseThrow()),
        () -> assertTrue(lexer.tryParse(".bar.").isEmpty())
        );
  }
  @Tag("Q5") @Test
  public void testCreateSubGroup() {
    var lexer = Lexer.create()
        .with("(9)X?X?", Integer::parseInt)
        .with("(7)X?X?", Double::parseDouble);
    assertAll(
        () -> assertEquals(7.0, lexer.tryParse("7").orElseThrow()),
        () -> assertEquals(9, lexer.tryParse("9X").orElseThrow()),
        () -> assertEquals(7.0, lexer.tryParse("7XX").orElseThrow()),
        () -> assertTrue(lexer.tryParse("XXX").isEmpty())
        );
  }
  @Tag("Q5") @Test
  public void testCreateOneWith() {
    var lexer = Lexer.<Integer>create()
        .with("(3)X?X?", Integer::parseInt);
    assertAll(
        () -> assertEquals(3, (int)lexer.tryParse("3").orElseThrow()),
        () -> assertTrue(lexer.tryParse("XXX").isEmpty())
        );
  }

}

