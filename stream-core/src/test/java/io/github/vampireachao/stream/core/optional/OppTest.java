package io.github.vampireachao.stream.core.optional;

import io.github.vampireachao.stream.core.lambda.function.SerRunn;
import io.github.vampireachao.stream.core.reflect.AbstractTypeReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * @author VampireAchao
 * @since 2022/6/2 19:06
 */
class OppTest {

    @Test
    void blankTest() {
        // blank相对于ofNullable考虑了字符串为空串的情况
        final String hutool = Opp.blank("").orElse("hutool");
        Assertions.assertEquals("hutool", hutool);
    }

    @Test
    void getTest() {
        // 和原版Optional有区别的是，get不会抛出NoSuchElementException
        // 如果想使用原版Optional中的get这样，获取一个一定不为空的值，则应该使用orElseThrow
        final Object opp = Opp.of(null).get();
        Assertions.assertNull(opp);
    }

    @Test
    void isEmptyTest() {
        // 这是jdk11 Optional中的新函数，直接照搬了过来
        // 判断包裹内元素是否为空，注意并没有判断空字符串的情况
        final boolean isEmpty = Opp.empty().isNull();
        Assertions.assertTrue(isEmpty);
    }

    @Test
    void peekTest() {
        final User user = new User();
        // 相当于ifPresent的链式调用
        Opp.of("hutool").peek(user::setUsername).peek(user::setNickname);
        Assertions.assertEquals("hutool", user.getNickname());
        Assertions.assertEquals("hutool", user.getUsername());

        // 注意，传入的lambda中，对包裹内的元素执行赋值操作并不会影响到原来的元素
        final String name = Opp.of("hutool").peek(username -> username = "123").peek(username -> username = "456").get();
        Assertions.assertEquals("hutool", name);
    }

    @Test
    void peeksTest() {
        final User user = new User();
        // 相当于上面peek的动态参数调用，更加灵活，你可以像操作数组一样去动态设置中间的步骤，也可以使用这种方式去编写你的代码
        // 可以一行搞定
        Opp.of("hutool").peeks(user::setUsername, user::setNickname);
        // 也可以在适当的地方换行使得代码的可读性提高
        Opp.required(user).peeks(
                u -> Assertions.assertEquals("hutool", u.getNickname()),
                u -> Assertions.assertEquals("hutool", u.getUsername())
        );
        Assertions.assertEquals("hutool", user.getNickname());
        Assertions.assertEquals("hutool", user.getUsername());

        // 注意，传入的lambda中，对包裹内的元素执行赋值操作并不会影响到原来的元素,这是java语言的特性。。。
        // 这也是为什么我们需要getter和setter而不直接给bean中的属性赋值中的其中一个原因
        final String name = Opp.of("hutool").peeks(
                username -> username = "123", username -> username = "456",
                n -> Assertions.assertEquals("hutool", n)).get();
        Assertions.assertEquals("hutool", name);

        // 当然，以下情况不会抛出NPE，但也没什么意义
        Opp.of("hutool").peeks().peeks().peeks();
        Opp.of(null).peeks(i -> {
        });

    }

    @Test
    void orTest() {
        // 这是jdk9 Optional中的新函数，直接照搬了过来
        // 给一个替代的Opp
        final String str = Opp.<String>of(null).or(() -> Opp.of("Hello hutool!")).map(String::toUpperCase).orElseThrow();
        Assertions.assertEquals("HELLO HUTOOL!", str);

        final User user = User.builder().username("hutool").build();
        final Opp<User> userOpp = Opp.required(user);
        // 获取昵称，获取不到则获取用户名
        final String name = userOpp.map(User::getNickname).or(() -> userOpp.map(User::getUsername)).get();
        Assertions.assertEquals("hutool", name);

        final String strOpt = Opp.of(Optional.of("Hello hutool!")).map(String::toUpperCase).orElseThrow();
        Assertions.assertEquals("HELLO HUTOOL!", strOpt);
    }

    @Test
    void orElseThrowTest() {
        Opp<Object> opp = Opp.of(null);
        // 获取一个不可能为空的值，否则抛出NoSuchElementException异常
        Assertions.assertThrows(NoSuchElementException.class, opp::orElseThrow);
        // 获取一个不可能为空的值，否则抛出自定义异常
        Assertions.assertThrows(IllegalStateException.class,
                () -> opp.orElseThrow(IllegalStateException::new));
    }

    @Test
    void orElseRunTest() {
        // 判断一个值是否为空，为空执行一段逻辑,否则执行另一段逻辑
        final Map<String, Integer> map = new HashMap<>();
        final String key = "key";
        map.put(key, 1);
        Opp.of(map.get(key))
                .ifPresent(v -> map.put(key, v + 1))
                .orElseRun(() -> map.remove(key));
        Assertions.assertEquals((Object) 2, map.get(key));
    }

    @Test
    void flattedMapTest() {
        // 和Optional兼容的flatMap
        final List<User> userList = new ArrayList<>();
        // 以前，不兼容
//		Opp.ofNullable(userList).map(List::stream).flatMap(Stream::findFirst);
        // 现在，兼容
        final User user = Opp.of(userList).map(List::stream)
                .flattedMap(Stream::findFirst).orElseGet(User.builder()::build);
        Assertions.assertNull(user.getUsername());
        Assertions.assertNull(user.getNickname());
    }

    @Test
    void emptyTest() {
        // 以前，输入一个CollectionUtil感觉要命，类似前缀的类一大堆，代码补全形同虚设(在项目中起码要输入完CollectionUtil才能在第一个调出这个函数)
        // 关键它还很常用，判空和判空集合真的太常用了...
        final List<String> past = Opp.of(Collections.<String>emptyList()).filter(l -> !l.isEmpty()).orElseGet(() -> Collections.singletonList("hutool"));
        // 现在，一个empty搞定
        final List<String> hutool = Opp.empty(Collections.<String>emptyList()).orElseGet(() -> Collections.singletonList("hutool"));
        Assertions.assertEquals(past, hutool);
        Assertions.assertEquals(Collections.singletonList("hutool"), hutool);
        Assertions.assertTrue(Opp.empty(Arrays.asList(null, null, null)).isNull());
    }

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ConstantConditions"})
    @Test
    void failOrElseTest() {
        // 有一些资深的程序员跟我说你这个lambda，双冒号语法糖看不懂...
        // 为了尊重资深程序员的意见，并且提升代码可读性，封装了一下 "try catch NPE 和 数组越界"的情况

        // 以前这种写法，简洁但可读性稍低，对资深程序员不太友好
        final List<String> last = null;
        final String npeSituation = Opp.empty(last).flattedMap(l -> l.stream().findFirst()).orElse("hutool");
        final String indexOutSituation = Opp.empty(last).map(l -> l.get(0)).orElse("hutool");

        // 现在代码整洁度降低，但可读性up，如果再人说看不懂这代码...
        final String npe = Opp.ofTry(() -> last.get(0)).failOrElse("hutool");
        final String indexOut = Opp.ofTry(() -> {
            final List<String> list = new ArrayList<>();
            // 你可以在里面写一长串调用链 list.get(0).getUser().getId()
            return list.get(0);
        }).failOrElse("hutool");
        Assertions.assertEquals(npe, npeSituation);
        Assertions.assertEquals(indexOut, indexOutSituation);
        Assertions.assertEquals("hutool", npe);
        Assertions.assertEquals("hutool", indexOut);

        // 多线程下情况测试
        Stream.iterate(0, i -> ++i).limit(20000).parallel().forEach(i -> {
            final Opp<Object> opp = Opp.ofTry(() -> {
                if (i % 2 == 0) {
                    throw new IllegalStateException(i + "");
                } else {
                    throw new NullPointerException(i + "");
                }
            });
            Assertions.assertTrue(
                    (i % 2 == 0 && opp.getException() instanceof IllegalStateException) ||
                            (i % 2 != 0 && opp.getException() instanceof NullPointerException)
            );
        });
    }

    @Test
    void testEmpty() {
        Assertions.assertTrue(Opp.empty(Arrays.asList(null, null, null)).isNull());
        Assertions.assertTrue(Opp.empty(Arrays.asList(null, 1, null)).isNonNull());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class User {
        private String username;
        private String nickname;
    }

    @Test
    void testTypeOfPeek() {
        Stream.<SerRunn>of(() -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<String> opp = Opp.of("").typeOfPeek((String str) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<String> opp = Opp.of("").typeOfPeek(Object.class, (Object str) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<int[]> opp = Opp.of(new int[]{1, 2}).typeOfPeek((int[] array) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<List<Integer>> opp = Opp.of(Arrays.asList(1, 2, 3, 4)).typeOfPeek((List<Integer> array) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<List<Integer>> opp = Opp.of(Arrays.asList(1, 2, 3)).typeOfPeek(List.class, (array) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<Map<Integer, String>> opp = Opp.of(Collections.singletonMap(1, "")).typeOfPeek(new AbstractTypeReference<Map<Integer, String>>() {}.getClass(), (array) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<Map<Integer, String>> opp = Opp.of(Collections.singletonMap(1, "")).typeOfPeek(new AbstractTypeReference<Map<Integer, String>>() {}.getClass(), (array) -> isExecute.set(true));
            Assertions.assertTrue(opp.isNonNull());
            Assertions.assertTrue(isExecute.get());
        }).forEach(SerRunn::run);
    }

    @Test
    void testTypeOfMap() {
        Stream.<SerRunn>of(() -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<Boolean> opp = Opp.of("").typeOfMap((String str) -> {
                isExecute.set(true);
                return isExecute.get();
            });
            Assertions.assertTrue(opp.get());
        }, () -> {
            AtomicBoolean isExecute = new AtomicBoolean();
            Opp<Boolean> opp = Opp.of("").typeOfMap((String str) -> {
                isExecute.set(true);
                return isExecute.get();
            }).typeOfMap(Object.class, i -> false).typeOfMap(new AbstractTypeReference<String>() {}, i -> true);
            Assertions.assertTrue(opp.isNull());
        }).forEach(SerRunn::run);
    }


    @Test
    void testTypeOfFilter() {
        Stream.<SerRunn>of(() -> {
            Opp<String> opp = Opp.of("").typeOfFilter((String str) -> str.trim().isEmpty());
            Assertions.assertTrue(opp.isNonNull());
        }, () -> {
            Opp<String> opp = Opp.of("").typeOfFilter((String str) -> !str.trim().isEmpty());
            Assertions.assertTrue(opp.isNull());
        }).forEach(SerRunn::run);
    }

    @Test
    void testIsEqual() {
        Assertions.assertTrue(Opp.of(1).isEqual(1));
    }

    @Test
    void testZip() {
        Stream.<SerRunn>of(() -> {
            String biMap = Opp.of(1).zip(Opp.of("st"), (l, r) -> l + r).get();
            Assertions.assertEquals("1st", biMap);
        }, () -> {
            String biMap = Opp.of(1).zip(Opp.<String>empty(), (l, r) -> l + r).get();
            Assertions.assertNull(biMap);
        }).forEach(SerRunn::run);
    }

    @Test
    void testZipOrSelf() {
        Stream.<SerRunn>of(() -> {
            String compose = Opp.blank("Vampire").zipOrSelf(Opp.of("Achao"), String::concat).get();
            Assertions.assertEquals("VampireAchao", compose);
        }, () -> {
            String compose = Opp.blank("Vampire").zipOrSelf(Opp.empty(), String::concat).get();
            Assertions.assertEquals("Vampire", compose);
        }).forEach(SerRunn::run);
    }
}
