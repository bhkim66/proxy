# 동적 프록시, 프록시 팩토리

## 리플렉션

자바가 기본으로 제공하는 JDK 동적 프록시 기술이나 CGLIB 같은 프록시 생성 오픈소스 기술을 활용하면 프록시 객체를 동적으로 만들어낼 수 있다.

JDK 동적 프록시를 이해하기 위해서는 먼저 자바의 리플렉션 기술을 이해해야 한다.

```java
@Test
void reflection1() throws Exception {
	//클래스 정보
	Class classHello = Class.forName("hello.proxy.jdkdynamic.ReflectionTest$Hello");
	Hello target = new Hello();
	
	//callA 메서드 정보
	Method methodCallA = classHello.getMethod("callA");
	Object result1 = methodCallA.invoke(target);
	log.info("result1={}", result1);
	
	//callB 메서드 정보
	Method methodCallB = classHello.getMethod("callB");
	Object result2 = methodCallB.invoke(target);
	log.info("result2={}", result2);
}
```

- `classHello.getMethod("call")` : 해당 클래스의 `call` 메서드 메타정보를 획득한다
- 기존의 `callA()` , `callB()` 메서드를 직접 호출하는 부분이 `Method` 로 대체되었다. 덕분에 이제 공통 로직을 만들수 있다

```java
@Test
void reflection2() throws Exception {
	Class classHello = Class.forName("hello.proxy.jdkdynamic.ReflectionTest$Hello");
	Hello target = new Hello();
	Method methodCallA = classHello.getMethod("callA");
	dynamicCall(methodCallA, target);
	Method methodCallB = classHello.getMethod("callB");
	dynamicCall(methodCallB, target);
}

private void dynamicCall(Method method, Object target) throws Exception {
	log.info("start");
	Object result = method.invoke(target);
	log.info("result={}", result);
}
```

- 정적인 `target.call(A)`, `target.callB()` 코드를 리플렉션을 사용해서 `Method`라는 메타정보로 추상화 했다
- 리플렉션을 사용하면 클래스와 메서드의 메타정보를 사용해서 애플리케이션을 동적으로 유연하게 만들 수 있다. 하지만 리플렉션 기술은 런타임에 동작하기 때문에, 컴파일 시점에 오류를 잡을 수 없다.

## JDK 동적 프록시

- 동적 프록시 기술을 사용하면 개발자가 직접 프록시 클래스를 만들지 않아도 된다. 이름 그대로 프록시 객체를 동적으로 런타임에 개발자 대신 만들어준다. 그리고 동적 프록시에 원하는 실행 로직을 지정할 수 있다
- JDK 동적 프록시는 인터페이스를 기반으로 프록시를 동적으로 만들어준다. 따라서 인터페이스가 필수이다

### InvocationHandler

JDK 동적 프록시에 적용할 로직은 InvocationHandler 인터페이스를 구현해서 작성하면 된다

```java
public interface InvocationHandler {
	public Object invoke(Object proxy, Method method, Object[] args)
	throws Throwable;
}
```

- Object proxy : 프록시 자신
- Method method : 호출한 메서드
- Object[] args : 메서드를 호출할 때 전달한 인수

```java
@Slf4j
public class TimeInvocationHandler implements InvocationHandler {
	private final Object target;
	
	public TimeInvocationHandler(Object target) {
		this.target = target;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		log.info("TimeProxy 실행");
		
		long startTime = System.currentTimeMillis();
		Object result = method.invoke(target, args);
		long endTime = System.currentTimeMillis();
		long resultTime = endTime - startTime;
		
		log.info("TimeProxy 종료 resultTime={}", resultTime);
		
		return result;
	}
}
```

- `method.invoke(target, args)` : 리플렉션을 사용해서 `target` 인스턴스의 메서드를 실행한다. `args`는 메서드 호출시 넘겨줄 인수이다.

```java
@Slf4j
public class JdkDynamicProxyTest {

@Test
void dynamicA() {
	AInterface target = new AImpl();
	TimeInvocationHandler handler = new TimeInvocationHandler(target);
	AInterface proxy = (AInterface)
	Proxy.newProxyInstance(AInterface.class.getClassLoader(), new Class[] {AInterface.class}, handler);
	proxy.call();
	
	log.info("targetClass={}", target.getClass());
	log.info("proxyClass={}", proxy.getClass());
	}
	
@Test
void dynamicB() {
	BInterface target = new BImpl();
	TimeInvocationHandler handler = new TimeInvocationHandler(target);
	BInterface proxy = (BInterface)
	Proxy.newProxyInstance(BInterface.class.getClassLoader(), new Class[]{BInterface.class}, handler);
	proxy.call();
	
	log.info("targetClass={}", target.getClass());
	log.info("proxyClass={}", proxy.getClass());
}

//dynamicA() 실행 결과
TimeInvocationHandler - TimeProxy 실행
AImpl - A 호출
TimeInvocationHandler - TimeProxy 종료 resultTime=0
JdkDynamicProxyTest - targetClass=class hello.proxy.jdkdynamic.code.AImpl
JdkDynamicProxyTest - proxyClass=class com.sun.proxy.$Proxy1
```

- new TimeInvocationHandler(target) : 동적 프록시에 적용할 핸들러 로직
- Proxy.newProxyInstance(AInterface.class.getClassLoader(), new Class[] {AInterface.class}, handler)
    - 클래스 로더 정보, 인퍼테이스, 그리고 핸들러 로직을 넣어주면 된다. 그러면 해당 인터페이스를 기반으로 동적 프록시를 생성하고 그 결과를 반환한다
- `proxyClass=class com.sun.proxy.$Proxy1` 이 부분이 동적으로 생성된 프록시 클래스 정보이다. 이것은 우리가 만든 클래스가 아니라 JDK 동적 프록시가 이름 그대로 동적으로 만들어준 프록시이다


**동적 프록시 클래스 정보**

`dynamicA()`와 `dynamicB()` 둘을 동시에 함께 실행하면 JDK 동적 프록시가 각각 다른 동적 프록시 클래스를 만들어주는 것을 확인할 수 있다

### JDK 동적 프록시의 한계

- JDK 동적 프록시는 인터페이스가 필수이다
- 인터페이스가 없이 클래스만 있는 경우에는 `CGLIB`라는 바이트코드를 조작하는 특별한 라이브러리를 사용해야 한다

## CGLIB

- CGLIB는 바이트코들르 조작해서 동적으로 클래스를 생성하는 기술을 제공하는 라이브러리이다
- CGLIB를 사용하면 인터페이스가 없어도 구체 클래스만 가지고 동적 프록시를 만들어낼 수 있다
- JDK 동적 프록시에서 실행 로직을 위해 `InvocationHandler` 를 제공했듯이, CGLIB는 `MethodInterceptor`를 제공한다.

```java
public interface MethodInterceptor extends Callback {
	Object intercept(Object obj, Method method, Object[] args, MethodProxy
proxy) throws Throwable;
```

- obj : CGLIB가 적용된 객체
- method : 호출된 메서드
- args : 메서드를 호출하면서 전달된 인수
- proxy : 메서드 호출에 사용

```java
@Slf4j
public class TimeMethodInterceptor implements MethodInterceptor {
	private final Object target;
	
	public TimeMethodInterceptor(Object target) {
		this.target = target;
	}
	
	@Override
	public Object intercept(Object obj, Method method, Object[] args,
	MethodProxy proxy) throws Throwable {
		log.info("TimeProxy 실행");
		
		long startTime = System.currentTimeMillis();
		Object result = proxy.invoke(target, args);
		long endTime = System.currentTimeMillis();
		long resultTime = endTime - startTime;
		
		log.info("TimeProxy 종료 resultTime={}", resultTime);
		return result;
	}
}
```

- `proxy.invoke(target, args)` : 실제 대상을 동적으로 호출한다.
    - 참고로 `method`를 사용해도 되지만, CGLIB는 성능상 `MethodProxy proxy`를 사용하는 것을 권장한다

## 프록시 팩토리

스프링은 유사한 구체적인 기술들이 있을 때, 그것들을 통합해서 일관성 있게 접근할 수 있고, 더욱 편리하게 사용할 수 있는 추상화된 기술을 제공한다

- 스프링은 동적 프록시를 통합해서 편리하게 만들어주는 프록시 팩토리(`ProxyFactory`)라는 기능을 제공한다
- 이전에는 상황에 따라서 JDK 동적 프록시를 사용하거나 CGLIB를 사용해야 했다면, **이제는 이 프록시 팩토리 하나로 편리하게 동적 프록시를 생성할 수 있다**

**프록시 팩토리 - 의존 관계**


**프록시 팩토리 - 사용 흐름**


- 개발자는 InvocationgHandler나 MethodInterceptor를 신경쓰지 않고, `Advice`만 만들면 된다
    - 결과적으로 `InvocationHandler` 나 `MethodInterceptor` 는 `Advice` 를 호출하게 된다

**Advice 도입**


**Advice 도입 전체 흐름**


- `Advice`는 프록시에 적용하는 부가 기능 로직이다. 이것은 JDK 동적 프록시가 제공하는 `InvocationHandler`와 CGLIB가 제공하는 `MethodInterceptor`의 개념과 유사한다. 둘의 개념을 추상화 한 것이다

**MethodInterceptor - 스프링이 제공하는 코드**

```java
package org.aopalliance.intercept;

public interface MethodInterceptor extends Interceptor {
	Object invoke(MethodInvocation invocation) throws Throwable;
}
```

- MethodInvocation invocation
    - 내부에 다음 메서드를 호출하는 방법, 현재 프록시 객체 인스턴스, args, 메서드 정보 등이 포함되어 있다
- 여기서 사용하는 `org.aopalliance.intercept` 패키지는 스프링 AOP 모듈( `spring-aop` ) 안에 들어있다.
- `MethodInterceptor` 는 `Interceptor` 를 상속하고 `Interceptor` 는 `Advice` 인터페이스를 상속한다.

```java
@Slf4j
public class TimeAdvice implements MethodInterceptor {

@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		log.info("TimeProxy 실행");
		
		long startTime = System.currentTimeMillis();
		Object result = invocation.proceed();
		long endTime = System.currentTimeMillis();
		long resultTime = endTime - startTime;
		
		log.info("TimeProxy 종료 resultTime={}ms", resultTime);
		return result;
		}
	}
```

- `Object result = invocation.proceed()`
    - `invocation.proceed()` 를 호출하면 `target` 클래스를 호출하고 그 결과를 받는다.
    - target 클래스 정보는 MethodInvocation invocation안에 모두  포함되어 있다
        - 프록시 팩토리로 프록시를 생성하는 단계에서 이미 `target` 정보를 파라미터로 전달받기 때문이다.

```java
public class ProxyFactoryTest {

	@Test
	@DisplayName("인터페이스가 있으면 JDK 동적 프록시 사용")
	void interfaceProxy() {
		ServiceInterface target = new ServiceImpl();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.addAdvice(new TimeAdvice());
		
		ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();
		log.info("targetClass={}", target.getClass());
		log.info("proxyClass={}", proxy.getClass());
		proxy.save();
		}
	}
```

- new ProxyFactory(target) : 프록시 팩토리를 생성할 때, 생성자에 프록시의 호출 대상을 함께 넘겨준다. 프록시 팩토리는 이 인스턴스 정보를 기반으로 프록시를 만들어낸다.
    - 만약 이 인스턴스에 인터페이스가 있다면 JDK 동적 프록시를 기본으로 사용하고 인터페이스가 없고 구체 클래스만 있다면 CGLIB를 통해서 동적 프록시를 생성한다

```java
@Test
@DisplayName("ProxyTargetClass 옵션을 사용하면 인터페이스가 있어도 CGLIB를 사용하고, 클래스 기
반 프록시 사용")
void proxyTargetClass() {
	ServiceInterface target = new ServiceImpl();
	ProxyFactory proxyFactory = new ProxyFactory(target);
	
	proxyFactory.setProxyTargetClass(true); //중요
	proxyFactory.addAdvice(new TimeAdvice());
	ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();
	
	log.info("targetClass={}", target.getClass());
	log.info("proxyClass={}", proxy.getClass());
	proxy.save();
}
```

- 프록시 팩토리는 `proxyTargetClass`라는 옵션을 제공하는데, 이 옵션에 true 값을 넣으면 인터페이스가 있어도 강제로 CGLIB를 사용한다. 그리고 인터페이스가 아닌 클래스 기반의 프록시를 만들어준다

### 프록시 팩토리의 기술 선택 방법

- 대상에 인터페이스가 있으면 : JDK 동적 프록시, 인터페이스 기반 프록시
- 대상에 인터페이스가 없으면 : CGLIB, 구체 클래스 기반 프록시
- `proxyTargetClass=true` : CGLIB, 구체 클래스 기반 프록시, 인터페이스 여부와 상관없음

> 참고 : 스프링 부트는 AOP를 적용할 때 기본적으로 `proxyTargetClass=true`를 설정해서 사용한다. 따라서 인터페이스가 있어도 항상 CGLIB를 사용해서 구체 클래스를 기반으로 프록시를 생성한다
> 

## 포인트컷, 어드바이스, 어드바이저

- **포인트컷**( `Pointcut` ): 어디에 부가 기능을 적용할지, 어디에 부가 기능을 적용하지 않을지 판단하는 필터링 로직이다. 주로 클래스와 메서드 이름으로 필터링 한다. 이름 그대로 어떤 포인트(Point)에 기능을 적용할지 하지 않을지 잘라서(cut) 구분하는 것이다.
- **어드바이스**( `Advice` ): 이전에 본 것 처럼 프록시가 호출하는 부가 기능이다. 단순하게 프록시 로직이다.
- **어드바이저**( `Advisor` ): 단순하게 하나의 포인트컷과 하나의 어드바이스를 가지고 있는 것이다. 쉽게 이야기해서 **포인트컷1 + 어드바이스1**이다.
- **역활과 책임**
    - 포인트컷은 대상 여부를 확인하는 필터 역할만 담당한다
    - 어드바이스는 깔끔하게 부가 기능 로직만 담당한다
    - 둘을 합치면 어드바이저가 된다. 스프링의 어드바이저는 하나의 포인트컷 + 하나의 어드바이스로 구성된다

```java
@Slf4j
public class AdvisorTest {

	@Test
	void advisorTest1() {
		ServiceInterface target = new ServiceImpl();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(Pointcut.TRUE, new TimeAdvice());
		proxyFactory.addAdvisor(advisor);
		ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();
		
		proxy.save();
		proxy.find();
	}
}
```

- new DefaultPointcutAdvisor : Advisor 인터페이스의 가장 일반적인 구현체이다. 생성자를 통해 하나의 포인트컷과 하나의 어드바이스를 넣어주면 된다


### 포인트컷

포인트 컷은 메서드 이름을 기반으로 필터링 해주는 기능을 제공한다

- `NameMatchMethodPointcut` : 메서드 이름을 기반으로 매칭한다. 내부에서는 `PatternMatchUtils` 를 사용한다.
    - 예) `*xxx*` 허용
- `JdkRegexpMethodPointcut` : JDK 정규 표현식을 기반으로 포인트컷을 매칭한다.
- `TruePointcut` : 항상 참을 반환한다.
- `AnnotationMatchingPointcut` : 애노테이션으로 매칭한다.
- `AspectJExpressionPointcut` : aspectJ 표현식으로 매칭한다.
    - 실무에서 사용하기 편리하고 기능도 가장 많은 aspectJ 표현식을 기반으로 사용하는 `AspectJExpressionPointcut`을 사용하게 된다

### 프록시 팩토리 - 여러 어드바이저 적용 가능

- 스프링은 AOP를 적용할 때, 최적화를 진행해서 **프록시는 하나만 만들고, 하나의 프록시에 여러 어드바이저를 적용한다**

## 빈 후처리기

- `@Bean`이나 컴포넌트 스캔으로 스프링 빈을 등록하면 ,스프링은 대상 객체를 생성하고 스프링 컨테이너 내부의 빈 저장소에 등록한다. 이후에 스프링 컨테이너를 통해 등록한 스프링 빈을 조회해서 사용하면 된다

### 빈 후처리기

- 스프링이 빈 저장소에 등록할 목적으로 생성한 객체를 빈 저장소에 등록하기 직전에 조작하고 싶다면 빈 후처리기를 사용하면 된다

빈 후처리기 과정

1. **생성** : 스프링 빈 대상이 되는 객체를 생성한다 (@Bean, 컴포넌트 스캔 모두 포함)
2. **전달** : 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달한다
3. **후 처리 작업** : 빈 후처리기는 전달된 스프링 빈 객체를 조작하거나 다른 객체로 바꿔치기 할 수 있다
4. **등록** : 빈 후처리기는 빈을 반환한다. 전달 된 빈은 그대로 반환하면 해당 빈이 등록되고, 바꿔치기 하면 다른 객체가 빈 저장소에 등록된다

- 일반적으로 스프링 컨테이너가 등록하는, 특히 컴포넌트 스캔의 대상이 되는 빈들은 중간에 조작할 방법이 없는데, 빈 후처리기를 사용하면 개발자가 등록하는 모든 빈을 중간에 조작할 수 있다.
- **빈 객체를 프록시로 교체**하는 것도 가능하다는 뜻이다
- 빈 후처리기를 사용하면 수동으로 등록하는 빈은 물론이고, 컴포넌트 스캔을 사용하는 빈까지 모두 프록시를 적용할 수 있다

```java
@Slf4j
public class PackageLogTraceProxyPostProcessor implements BeanPostProcessor {
	private final String basePackage;
	private final Advisor advisor;

	public PackageLogTraceProxyPostProcessor(String basePackage, Advisor
	advisor) {
		this.basePackage = basePackage;
		this.advisor = advisor;
	}
	
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
																			throws BeansException {
		log.info("param beanName={} bean={}", beanName, bean.getClass());
		
		//프록시 적용 대상 여부 체크
		//프록시 적용 대상이 아니면 원본을 그대로 반환
		String packageName = bean.getClass().getPackageName();
		if (!packageName.startsWith(basePackage)) {
			return bean;
		}
		
		//프록시 대상이면 프록시를 만들어서 반환
		ProxyFactory proxyFactory = new ProxyFactory(bean);
		proxyFactory.addAdvisor(advisor);
		Object proxy = proxyFactory.getProxy();
		
		log.info("create proxy: target={} proxy={}", bean.getClass(),
		proxy.getClass());
		return proxy;
		}
	}
```

- 프록시 적용 대상의 반환 값을 보면 원본 객체 대신에 프록시 객체를 반환한다. 따라서 스프링 컨테이너에 원본 객체 대신에 프록시 객체가 스프링 빈으로 등록된다. 원본 객체는 스프링 빈으로 등록되지 않는다

```java
@Slf4j
@Configuration
@Import({AppV1Config.class, AppV2Config.class})
public class BeanPostProcessorConfig {
	
	@Bean
	public PackageLogTraceProxyPostProcessor logTraceProxyPostProcessor(LogTrace logTrace) {
		return new PackageLogTraceProxyPostProcessor("hello.proxy.app", getAdvisor(logTrace));
	}
	
	private Advisor getAdvisor(LogTrace logTrace) {
		//pointcut
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.setMappedNames("request*", "order*", "save*");
		
		//advice
		LogTraceAdvice advice = new LogTraceAdvice(logTrace);
		
		//advisor = pointcut + advice
		return new DefaultPointcutAdvisor(pointcut, advice);
	}
}
```

- `@Bean logTraceProxyPostProcessor()` : 특정 패키지를 기준으로 프록시를 생성하는 빈 후처리기를 스프링 빈으로 등록한다. 빈 후처리기는 스프링 빈으로만 등록하면 자동으로 동작한다. 여기에 프록시를 적용할 패키지 정보( `hello.proxy.app` )와 어드바이저( `getAdvisor(logTrace)` )를 넘겨준다.
- 이제 **프록시를 생성하는 코드가 설정 파일에는 필요 없다.** 순수한 빈 등록만 고민하면 된다. 프록시를 생성하고 프록시를 스프링 빈으로 등록하는 것은 빈 후처리기가 모두 처리해준다.
- 결과적으로 **포인트 컷**은 다음 두 곳에 사용된다.
    1. 프록시 적용 대상 여부를 체크해서 꼭 필요한 곳에만 프록시를 적용한다. (빈 후처리기 - 자동 프록시 생성) - ex) “hello.proxy.app”
    2. 프록시의 어떤 메서드가 호출 되었을 때 어드바이스를 적용할 지 판단한다. (프록시 내부) - ex) "request*", "order*", "save*"

### 스프링이 제공하는 빈 후처리기

**build.gradle - 추가**

```java
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

**자동 프록시 생성기 - AutoProxyCreator**

- 스프링 부트 자동 설정으로 `AnnotationAwareAspectJAutoProxyCreator` 라는 빈 후처리기가 스프링 빈에 자동으로 등록된다
- 자동으로 프록시를 생성해주는 빈 후처리기이다.
- 스프링 빈으로 등록된 `Advisor`들을 자동으로 찾아서 프록시가 필요한 곳에 자동으로 프록시를 적용해준다
- `Advisor` 안에는 `Pointcut` 과 `Advice` 가 이미 모두 포함되어 있다. 따라서 `Advisor` 만 알고 있으면 그 안에 있는 `Pointcut` 으로 어떤 스프링 빈에 프록시를 적용해야 할지 알 수 있다. 그리고 `Advice` 로 부가 기능을 적용하면 된다

**자동 프록시 생성기의 작동 과정**

1. 생성 : 스프링이 스프링 빈 대상이 되는 객체를 생성한다. (@Bean, 컴포넌트 스캔 모두 포함)
2. 전달 : 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달한다
3. 모든 Advisor 빈 조회 : 자동 프록시 생성기 - 빈 후처리기는 스프링 컨테이너에서 모든 Advisor를 조회한다
4. 프록시 적용 대상 체크 : 앞서 조회한 `Advisor` 에 포함되어 있는 포인트컷을 사용해서 해당 객체가 프록시를 적용할 대상인지 아닌지 판단한다. 이때 객체의 클래스 정보는 물론이고, 해당 객체의 모든 메서드를 포인트컷에 하나하나 모두 매칭해본다. 그래서 조건이 하나라도 만족하면 프록시 적용 대상이 된다
5. 프록시 생성: 프록시 적용 대상이면 프록시를 생성하고, 반환해서 프록시를 스프링 빈으로 등록한다. 만약 프록시 적용 대상이 아니라면 원본 객체를 반환해서 원본 객체를 스프링 빈으로 등록한다.
6. 빈 등록: 반환된 객체는 스프링 빈으로 등록된다

### 하나의 프록시, 여러 Advisor 적용

프록시 자동 생성기는 프록시를 하나만 생성한다. 왜나하면 프록시 팩토리가 생성하는 프록시 내부에 여러 `advisor`들을 포함할 수 있기 때문이다. 따라서 프록시를 여러개 생성해서 비용을 낭비할 이유가 없다

**프록시 자동 생성기 상황별 정리**

- advisor1의 포인트컷만 만족 → 프록시 1개 생성, 프록시에 advisor1만 포함
- advisor1, advisor2의 포인트컷을 모두 만족 → 프록시 1개 생성, 프록시에 advisor1, advisor2 모두 포함
- advisor1, advisor2의 포인트컷을 모두 만족하지 않음 → 프록시가 생성되지 않음

**하나의 프록시, 여러 어드바이저**


## Aspect

- 스프링은 `@Aspect` 어노테이션으로 매우 편리하게 포인트컷과 어드바이스로 구성되어 있는 어드바이저 생성 기능을 지원한다
- `@Aspect` 는 관점 지향 프로그래밍(AOP)을 가능하게 하는 AspectJ 프로젝트에서 제공하는 어노테이션이다
- `AnnotationAwareAspectJAutoProxyCreator` 는 `Advisor`를 자동으로 찾아와서 필요한 곳에 프록시를 생성하고 적용해준다. 그리고 추가로 `@Aspect` 를 찾아서 이것을 `Advisor`로 만들어준다


**1. @Aspect를 어드바이저로 변환해서 저장하는 과정**


1. **실행** : 스프링 애플리케이션 로딩 시점에 자동 프록시 생성기를 호출한다
2. **모든 @Aspect 빈 조회** : 자동 프록시 생성기는 스프링 컨테이너에서 @Aspect 어노테이션이 붙은 스프링 빈을 모두 조회한다
3. **어드바이저 생성** : @Aspect 어드바이저 빌더를 통해 @Aspect 어노테이션 정보를 기반으로 어드바이저를 생성한다
4. **@Aspect 기반 어드바이저 저장** : 생성한 어드바이저를 @Aspect 어드바이저 빌더 내부에 저장한다

**@Aspect 어드바이저 빌더**

`BeanFactoryAspectJAdvisorsBuilder` 클래스이다. `@Aspect` 의 정보를 기반으로 포인트컷, 어드바이스, 어드바이저를 생성하고 보관하는 것을 담당한다. `@Aspect` 의 정보를 기반으로 어드바이저를 만들고, @Aspect 어드바이저 빌더 내부 저장소에 캐시한다. 캐시에 어드바이저가 이미 만들어져 있는 경우 캐시에 저장된 어드바이저를 반환한다.

**2. 어드바이저 기반으로 프록시 생성**

1. **생성** : 스프링 빈 대상이 되는 객체를 생성한다. (@Bean, 컴포넌트 스캔 모두 포함)
2. **전달** : 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달한다

3-1. **Advisor 빈 조회** : 스프링 컨테이너에서 `Advisor` 빈을 모두 조회한다

3-2. **@Aspect Advisor 조회** : `@Aspect` 어드바이저 빌더 내부에 저장된 `Advisor`를 모두 조회한다

1. **프록시 적용 대상 체크** : 앞서 3-1, 3-2에서 조회한 `Advisor` 에 포함되어 있는 포인트컷을 사용해서 해당 객체가 프록시를 적용할 대상인지 아닌지 판단한다. 이때 객체의 클래스 정보는 물론이고, 해당 객체의 모든 메서드를 포인트컷에 하나하나 모두 매칭해본다. **그래서 조건이 하나라도 만족하면 프록시 적용 대상이 된다**. 예를 들어 서 메서드 하나만 포인트컷 조건에 만족해도 프록시 적용 대상이 된다.
2. **프록시 생성** : 프록시 적용 대상이면 프록시를 생성하고 프록시를 반환한다. 그래서 프록시를 스프링 빈으로 등록한다. 만약 프록시 대상이 아니라면 원본 객체를 반환해서 **원본 객체를 스프링 빈으로 등록**한다
3. **빈 등록** : 반환된 객체는 스프링 빈으로 등록된다


# 프록시 패턴

## 프록시

- 클라이언트와 서버의 기본 개념을 정의하면 클라이언**트는 서버에 필요한 것을 요청하고, 서버는 클라이언트에 요청을 처리** 하는 것이다
- 컴퓨터 네트워크에 적용하면 클라이언트는 `웹 브라우저`가 되고, 요청을 처리하는 서버는 `웹 서버`가 된다
- 그런데 클라이언트가 요청한 결과를 서버에 직접 요청하는 것이 아니라 **어떤 대리자를 통해서 대신 간접적으로 서버에 요청**하는 것을 `프록시`(대리자)라고 한다


**대체 가능**

- 객체에서 프록시가 되려면, 클라이언트는 서버에게 요청을 한 것인지, 프록시에게 요청을 한 것인지 조차 몰라야한다
    - 서버와 프록시는 같은 인터페이스를 사용해야 한다
    - 클라이언트가 사용하는 **서버 객체를 프록시 객체로 변경해도 클라이언트 코드를 변경하지 않고 동작할 수 있어야 한다**


### 프록시의 주요 기능

- **접근 제어**
    - 권한에 따른 접근 차단
    - 캐싱
    - 지연 로딩
- **부가 기능 추가**
    - 원래 서버가 제공하는 기능에 더해서 부가 기능을 수행한다
    - 예) 요청 값이나, 응답 값을 중간에 변형한다.
    - 예) 실행 시간을 측정해서 추가 로그를 남긴다.
- **GOF 디자인 패턴**
    - 둘다 프록시를 사용하는 방법이지만 `GOF 디자인 패턴`에서는 이 둘을 의도에 따라서 프록시 패턴과 데코레이터 패턴으로 구분한다
    - **프록시 패턴** : 접근 제어가 목적
    - **데코레이터 패턴** : 새로운 기능 추가가 목적

## 프록시 패턴


```java
@Slf4j
public class CacheProxy implements Subject {
	private Subject target;
	private String cacheValue;
	
	public CacheProxy(Subject target) {
		this.target = target;
	}
	
	@Override
	public String operation() {
		log.info("프록시 호출");
		if (cacheValue == null) {
			cacheValue = target.operation();
		}
		return cacheValue;
	}
}
```

```java
@Slf4j
public class RealSubject implements Subject {
	@Override
	public String operation() {
		log.info("실제 객체 호출");
		sleep(1000);
		return "data";
	}
	
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
```

```java
public class ProxyPatternTest {
	@Test
	void noProxyTest() {
		RealSubject realSubject = new RealSubject();
		ProxyPatternClient client = new ProxyPatternClient(realSubject);
		client.execute();
		client.execute();
		client.execute();
	}
	
	@Test
	void cacheProxyTest() {
		Subject realSubject = new RealSubject();
		Subject cacheProxy = new CacheProxy(realSubject);
		ProxyPatternClient client = new ProxyPatternClient(cacheProxy);
		client.execute();
		client.execute();
		client.execute();
	}
}

//실행 결과
CacheProxy - 프록시 호출
RealSubject - 실제 객체 호출
CacheProxy - 프록시 호출
CacheProxy - 프록시 호출
```

**cacheProxyTest()**
`realSubject` 와 `cacheProxy` 를 생성하고 둘을 연결한다. 결과적으로 `cacheProxy` 가 `realSubject` 를 참조하는 런타임 객체 의존관계가 완성된다. 그리고 마지막으로 `client` 에 `realSubject` 가 아닌 `cacheProxy` 를 주입한다. 이 과정을 통해서 `client -> cacheProxy -> realSubject` 런타임 객체 의존 관계가 완성된다

**정리**

- 프록시 패턴의 핵심은 `RealSubject` 코드와 클라이언트 코드를 전혀 변경하지 않고, 프록시를 도입해서 접근 제어를 했다는 점이다
- 그리고 클라이언트 코드의 변경 없이 자유롭게 프록시를 넣고 뺄 수 있다. 실제 클라이언트 입장에서는 프록시 객체가 주입되었는지, 실제 객체가 주입되었는지 알지 못한다
