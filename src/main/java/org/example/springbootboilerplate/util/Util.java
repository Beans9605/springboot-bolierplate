package org.example.springbootboilerplate.util;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Util {

    /**
     * general 속성을 가진 Array 의 앞 뒤를 붙혀주는 Util 성 일급함수
     *
     * @param firstArray
     * @param secondArray
     * @return T[] - 전달 받은 두 Array 를 순차대로 합친 결과
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatArray(T[] firstArray, T[] secondArray) {
        return Stream.concat(Arrays.stream(firstArray), Arrays.stream(secondArray)).toArray(size ->
                (T[]) Array.newInstance(
                        firstArray.getClass().getComponentType(), size));
    }

    /**
     * general 속성을 가진 Array list 를 하나로 합쳐주는 함수 (갯 수 제한 없음)
     * Array 들은 같은 타입이여야지만 성립됨
     * @param arrays
     * @return
     * @param <T>
     */
    @SafeVarargs
    public static <T> T[] concatAllArray(T[] ...arrays) {
        T[] result = null;
        for (T[] array : arrays) {
            if (Objects.isNull(result)) {
                result = array;
            }
            else {
                result = concatArray(result, array);
            }
        }
        return result;
    }

    /**
     * 특정 kubeConfig 를 읽고 싶을 때, path 를 입력받아 사용, null 일 경우 defaultClient 사용
//     * @param kubeConfigPath kubeConfig path
     * @return ApiClient - K8S Api Client
     * @throws IOException
     */
    public static ApiClient getK8SAPIClient(@Nullable String kubeConfigPath) throws IOException {
        try {
            if (Objects.nonNull(kubeConfigPath)) {
                return Config.fromConfig(kubeConfigPath);
            }
            else {
                return Config.defaultClient();
            }
        }
        catch (Exception exception) {
            return Config.defaultClient();
        }
    }

    public static Map<String, Collection<String>> allowResolveHeader (Map<String, Collection<String>> requestHeader) {
        Set<String> keys = requestHeader.keySet();
        if (
                keys.contains(HttpHeaders.CONTENT_LENGTH.toLowerCase())) {
            requestHeader.remove(HttpHeaders.CONTENT_LENGTH.toLowerCase());
        }
        return requestHeader;
    }

    public static Resource stringToResource (String value) {
        return new ByteArrayResource(value.getBytes());
    }

    public static Map<String, String> getRequestHeaders() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            Enumeration<String> headers = request.getHeaderNames();
            Map<String, String> headersMap = new HashMap<String, String>();
            while (headers.hasMoreElements()) {
                String key = headers.nextElement();
                headersMap.put(key, request.getHeader(key));
            }
            return headersMap;
        }
        else {
            return RequestHolder.getRequestHeaders();
        }
    }
    /**
     * @implNote <p>
     *     pair 의 left 값이 Collection 이여야하고 right 값은 left Collection 값의 제너릭 타입을 같은 타입으로 가져야함.<br/>
     *     right Function 의 경우 left Collection 내부의 값을 추출 할 수 있는 Function 으로 구성해줘야 정확한 하나의 값을 얻을 수 있음.<br/>
     *     정확한 값으로 선언 및 사용했다면, Collection 에서 특정한 T class 의 중복 값이 하나도 없는 순수 하나의 객체와 그 값을 얻을 있음.<br/>
     *     만약 추출된 결과가 2개 이상 혹은 없다면 null 을 반환. - Collection 에서 특정 값 추출 및 중복 제거를 통한 하나의 객체 반환이 목적이기 때문
     * </p>
     *
     * @Example <pre>
     *     class Cat {
     *         String name;
     *
     *         public Cat(String name) {
     *             this.name = name;
     *         }
     *
     *         public String getName() {
     *             return this.name;
     *         }
     *     }
     *     class Dog {
     *         String name;
     *
     *         public Dog(String name) {
     *             this.name = name;
     *         }
     *
     *         public String getName() {
     *             return this.name;
     *         }
     *     }
     *
     *     public static void main(String[] args) {
     *         Cat cat = new Cat("name");
     *         Cat cat1 = new Cat("name");
     *         Cat cat2 = new Cat("name");
     *
     *         Dog dog = new Dog("name");
     *         Dog dog1 = new Dog("name");
     *         Dog dog2 = new Dog("name");
     *
     *         List< Cat > cats = new ArrayList<>(List.of(cat, cat1, cat2));
     *         List< Dog > dogs = new ArrayList<>(List.of(dog, dog1, dog2));
     *
     *         String distinctName = Util.getSingleDataByCollections(
     *              Pair.of(cats, (Function< Cat, String >) Cat::getName),
     *              Pair.of(dogs, (Function< Dog, String >) Dog::getName)
     *         );
     *
     *         System.out.println(distinctName);
     *         // "name"
     *     }
     * </pre>
     *
     * @param pairs {@link Pair} left: {@code ?}, right: {@link Function}
     * @return {@link T}
     * @param <T>
     */
    @SafeVarargs
    public static <T> T getSingleDataByCollections(Pair<?, Function<?, T>>... pairs) {
        try {
            if (pairs == null || pairs.length == 0) {
                return null;
            }
            Set<T> set = new HashSet<>();
            for (Pair<?, Function<?, T>> pair : pairs) {
                Collection<?> collection = (Collection<?>) pair.getLeft();
                @SuppressWarnings("unchecked")
                Function<Object, T> function = (Function<Object, T>) pair.getRight();
                set.addAll(collection.stream()
                        .filter(Objects::nonNull)
                        .map(function)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
            }
            return set.size() == 1 ? set.iterator().next() : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * @implNote <p>
     *      하나의 Collection 에서 두 List 으로 분기하는 메소드.<br/>
     *      getFlag 의 함수에 따라서 flag 와 값이 같을 경우 반환 Pair 의 Left List 의 값으로 넣어주고, 아닐 경우 Right List 의 값으로 넣음.<br/>
     *      따라서 Left 는 flag 값에 맞는 값, Right 는 다른 값을 넣게됨.<br/>
     *      flag 와 getFlag 의 반환 값에 대한 타입은 Generic 으로 두 타입만 같다면 사용 가능.
     * </p>
     *
     * @Example <pre>
     *        class Person {
     *          private String name;
     *          private String sex;
     *
     *          public Person(String name, String sex) {
     *              this.name = name;
     *              this.sex = sex;
     *          }
     *
     *          public String getSex() {
     *              return this.sex;
     *          }
     *        }
     *
     *        public static void main(String[] args) {
     *            Person james = new Person("james", "male");
     *            Person john = new Person("john", "male");
     *            Person anna = new Person("anna", "female");
     *
     *            List < Person > people = new ArrayList<>(List.of(james, john, anna));
     *
     *            Pair < List < Person >, List < Person > > pair =
     *              Util.getPairWithCollectionsByFlag(
     *                  people,
     *                  Person::getSex,
     *                  "male"
     *              );
     *            // pair.getLeft() -> james, john
     *            // pair.getRight() -> anna
     *        }
     * </pre>
     *
     * @param collection {@link Collection} < {@link T} >
     * @param getFlag {@link Function} < {@link T}, {@link V} >
     * @param flag {@link V} 반환 Pair 의 Left List 의 Item 값, 이 flag 가 아니라면 전부 Right
     * @return {@link Pair} < {@link List} < {@link T} >, {@link List} < {@link T} > >
     */
    public static <T, V> Pair<List<T>, List<T>> getPairWithCollectionsByFlag(
            Collection<T> collection,
            Function<T, V> getFlag,
            V flag)
    {
        if (collection == null || collection.isEmpty()) {
            return Pair.of(new ArrayList<>(), new ArrayList<>());
        }
        return collection.stream().reduce(
                Pair.of(new ArrayList<>(), new ArrayList<>()),
                (pre, t) -> {
                    if (getFlag.apply(t).equals(flag)) {
                        pre.getLeft().add(t);
                    } else {
                        pre.getRight().add(t);
                    }
                    return pre;
                },
                (pre, curr) -> {
                    pre.getLeft().addAll(curr.getLeft());
                    pre.getRight().addAll(curr.getRight());
                    return pre;
                }
        );
    }

    /**
     * @implNote <p>
     *      하나의 Collection 에서 두 List 으로 분기하는 메소드.<br/>
     *      getFlag 의 함수에 따라서 flag 와 값이 같을 경우 반환 Pair 의 Left List 의 값으로 넣어주고, other flag 일 경우 Right List 의 값으로 넣음.<br/>
     *      따라서 Left 는 flag 값에 맞는 값, Right 는 otherFlag 값에 맞는 값을 넣게됨.<br/>
     *      flag 와 getFlag 의 반환 값에 대한 타입은 Generic 으로 두 타입만 같다면 사용 가능.
     * </p>
     *
     * @param collection {@link Collection} < {@link T} >
     * @param getFlag {@link Function} < {@link T}, {@link V} >
     * @param flag {@link V} 반환 Pair 의 Left List 의 Item 값
     * @param otherFlag {@link V} 반환 Pair 의 Right List 의 Item 값
     * @return {@link Pair} < {@link List} < {@link T} >, {@link List} < {@link T} > >
     */
    public static <T, V> Pair<List<T>, List<T>> getPairWithCollectionsByFlag(
            Collection<T> collection,
            Function<T, V> getFlag,
            V flag,
            V otherFlag
    )
    {
        if (collection == null || collection.isEmpty()) {
            return Pair.of(new ArrayList<>(), new ArrayList<>());
        }
        return collection.stream().reduce(
                Pair.of(new ArrayList<>(), new ArrayList<>()),
                (pre, t) -> {
                    if (getFlag.apply(t).equals(flag)) {
                        pre.getLeft().add(t);
                    }
                    if (getFlag.apply(t).equals(otherFlag)) {
                        pre.getRight().add(t);
                    }
                    return pre;
                },
                (pre, curr) -> {
                    pre.getLeft().addAll(curr.getLeft());
                    pre.getRight().addAll(curr.getRight());
                    return pre;
                }
        );
    }

    /**
     * @implNote <p>
     *     프로젝트 유저 정보를 업데이트 하는 기준은 actionFlag 를 U, D, A 세가지 종류로 받아서 결정짓습니다.<br/>
     *     그에 따라 여러번 탐색하지 않고 한번의 탐색에 Update, Delete, Add Member 를 분류하여 사용하게끔 도와주는 메소드 입니다.<br/>
     *
     *     그 외 하나의 Collection 에서 세가지 flag 로 세 List 로 반환하고싶다면 사용할 수 있음.<br/>
     *     flag 와 getFlag 의 반환 값에 대한 타입은 Generic 으로 두 타입만 같다면 사용 가능.
     * </p>
     * @Example <pre>
     *        enum UserRole {
     *            ADMIN,
     *            MEMBER,
     *            GUEST;
     *
     *            public String getId() {
     *                return this.name();
     *            }
     *        }
     *
     *        class User {
     *          private String name;
     *          private UserRole role;
     *
     *          public User(String name, UserRole role) {
     *              this.name = name;
     *              this.role = role;
     *          }
     *
     *          public UserRole getUserRole() {
     *              return this.role;
     *          }
     *        }
     *
     *        public static void main(String[] args) {
     *            User james = new User("james", UserRole.ADMIN);
     *            User john = new User("john", UserRole.GUEST);
     *            User anna = new User("anna", UserRole.GUEST);
     *            User ben = new User("ben", UserRole.MEMBER);
     *            User kim = new User("kim", UserRole.MEMBER);
     *            User hooker = new User("hooker", UserRole.MEMBER);
     *
     *            List < User > users = new ArrayList<>(List.of(james, john, anna, ben, kim, hooker));
     *
     *            Triple < List < User >, List < User >, List < User > > triple =
     *              Util.getTripleWithCollectionsByFlag(
     *                  people,
     *                  User::getUserRole,
     *                  UserRole.ADMIN,
     *                  UserRole.MEMBER,
     *                  UserRole.GUEST
     *              );
     *            // pair.getLeft() -> james => UserRole.ADMIN
     *            // pair.getMiddle() -> ben, kim, hooker => UserRole.MEMBER
     *            // pair.getRight() -> john, anna => UserRole.GUEST
     *        }
     * </pre>
     * @param collection {@link Collection} {@link T}
     * @param getFlag {@link Function} < {@link T}, {@link V} >
     * @param flag {@link V}
     * @return {@link Triple} < {@link List} < {@link T}: flag >, {@link List} < {@link T}: flag1 >, {@link List} < {@link T}: flag2 > >
     */
    @SafeVarargs
    public static <T, V> Triple<List<T>, List<T>, List<T>> getTripleWithCollectionsByFlag(
            Collection<T> collection,
            Function<T, V> getFlag,
            V... flag
    )
    {
        if (collection == null || collection.isEmpty() || flag == null || flag.length == 0 || flag.length > 3) {
            return Triple.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        return collection.stream().reduce(
                Triple.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                (pre, t) -> {
                    if (flag.length == 3) {
                        if (getFlag.apply(t).equals(flag[0])) {
                            pre.getLeft().add(t);
                        } else if (getFlag.apply(t).equals(flag[1])) {
                            pre.getMiddle().add(t);
                        } else if (getFlag.apply(t).equals(flag[2])) {
                            pre.getRight().add(t);
                        }
                    }
                    else if (flag.length == 2) {
                        if (getFlag.apply(t).equals(flag[0])) {
                            pre.getLeft().add(t);
                        } else if (getFlag.apply(t).equals(flag[1])) {
                            pre.getMiddle().add(t);
                        } else {
                            pre.getRight().add(t);
                        }
                    }
                    else {
                        if (getFlag.apply(t).equals(flag[0])) {
                            pre.getLeft().add(t);
                        } else  {
                            pre.getMiddle().add(t);
                            pre.getRight().add(t);
                        }
                    }
                    return pre;
                },
                (pre, curr) -> {
                    pre.getLeft().addAll(curr.getLeft());
                    pre.getMiddle().addAll(curr.getMiddle());
                    pre.getRight().addAll(curr.getRight());
                    return pre;
                }
        );
    }

    public static <T, V, Y> Pair<List<V>, List<Y>> getPairListByFunctions(
            Collection<T> collection,
            Function<T, V> getLeft,
            Function<T, Y> getRight
    )
    {
        if (collection == null || collection.isEmpty()) {
            return Pair.of(new ArrayList<>(), new ArrayList<>());
        }
        return collection.stream().reduce(
                Pair.of(new ArrayList<>(), new ArrayList<>()),
                (pre, t) -> {
                    pre.getLeft().add(getLeft.apply(t));
                    pre.getRight().add(getRight.apply(t));
                    return pre;
                },
                (pre, curr) -> {
                    pre.getLeft().addAll(curr.getLeft());
                    pre.getRight().addAll(curr.getRight());
                    return pre;
                }
        );
    }

    /**
     * @implNote 하나의 Collection 에서 두 함수를 이용해 각 함수의 결과를 Pair 로 갖는 List 값을 반환
     *
     * @param collection {@code Collection}
     * @param getLeft {@code Function<T, V>}
     * @param getRight {@code Function<T, Y>}
     * @return {@code List<Pair<V, Y>>}
     * @param <T> {@code List, Set, Map} 등 {@code Collection}의 제너릭타입
     * @param <V> getLeft 의 결과로 나올 제너릭타입
     * @param <Y> getRight 의 결과로 나올 제너릭타입
     */
    public static <T, V, Y> List<Pair<V, Y>> getListPairByFunctions(
            Collection<T> collection,
            Function<T, V> getLeft,
            Function<T, Y> getRight
    )
    {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        return collection.stream().map(
                t -> Pair.of(getLeft.apply(t), getRight.apply(t))
        ).toList();
    }

    /**
     * @implNote 두 사이즈가 같은 리스트를 하나의 리스트 안에 각각 Pair 값으로 존재하게끔 만들어주는 메소드, 하나의 {@code List<Pair<T, V>>} 반환
     *
     * @param tCollection {@code List<T>}
     * @param vCollection {@code List<V>}
     * @return {@code List<Pair<T, V>>}
     * @param <T> 반환 {@code List<Pair<T, V>>} 의 Pair 의 Left 제너릭타입
     * @param <V> 반환 {@code List<Pair<T, V>>} 의 Pair 의 Right 제너릭타입
     */
    public static <T, V> List<Pair<T, V>> getListPairByTwoList(List<T> tCollection, List<V> vCollection) {
        if (tCollection == null || tCollection.isEmpty() || vCollection == null || vCollection.isEmpty()) {
            return new ArrayList<>();
        }
        if (tCollection.size() != vCollection.size()) {
            return new ArrayList<>();
        }
        return IntStream.range(0, tCollection.size())
                .mapToObj(i -> Pair.of(tCollection.get(i), vCollection.get(i)))
                .toList();
    }

    /**
     * @implNote 두 사이즈가 같은 Collection 을 하나의 List 안에 각각 Pair 값으로 존재하게끔 만들어주는 메소드, 하나의 {@code List<Pair<T, V>>} 반환
     *
     * @param tCollection {@code List<T>}
     * @param vCollection {@code List<V>}
     * @return {@code List<Pair<T, V>>}
     * @param <T> 반환 {@code List<Pair<T, V>>} 의 Pair 의 Left 제너릭타입
     * @param <V> 반환 {@code List<Pair<T, V>>} 의 Pair 의 Right 제너릭타입
     */
    public static <T, V> List<Pair<T, V>> getListPairByTwoCollection(Collection<T> tCollection, Collection<V> vCollection) {
        if (tCollection == null || tCollection.isEmpty() || vCollection == null || vCollection.isEmpty()) {
            return new ArrayList<>();
        }
        if (tCollection.size() != vCollection.size()) {
            return new ArrayList<>();
        }
        Iterator<T> titerator = tCollection.iterator();
        Iterator<V> viterator = vCollection.iterator();
        try {
            return IntStream.range(0, tCollection.size())
                    .mapToObj(i -> Pair.of(titerator.next(), viterator.next()))
                    .toList();
        }
        catch (NoSuchElementException e) {
            return new ArrayList<>();
        }
    }
}
