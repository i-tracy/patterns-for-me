## <center> 行为型 - 桥（Bridge）设计模式
---

几年前，在我刚接触设计模式的时候，公司的前辈告诉我：桥模式是所有设计模式中最难的设计模式。如今我回过头对我所认识的设计模式进行总结时，我倒认为桥模式反而是设计模式中比较简单的。今天，我们就来聊一聊桥模式（亦叫桥接模式）。

# 一、问题引入

为了对桥模式阐述的足够透彻，我将通过一个真实的案例进行举例。但为了让大家不陷入其他的旁枝末节，我将屏蔽那些看起来不太必要的细节，只保留核心的骨架。

> 当系统足够成熟时，用户总是希望能接收到来自于系统的通知。例如当我们购买的商品在物流信息更新时，用户希望在 APP 内能够提醒自己，以便随时关心物流动态。当商品到达配送点时，我们又通过手机短信或者机器人语音通话的方式被告知。同时，系统也许会在月初时，为用户整理在这一个月内 APP 内的浏览记录，统计月账单等整理成报表，并通过邮件发送给用户。

分析需求，我们知道对于一个消息通知来说，有 4 种通知方式可供选择。他们分别是：站内消息、邮件、短信和机器人语音通话。而他们的在实现上差异巨大，在很多时候我们还必须引入不同的第三方支撑才能完成。所以，在一个类中实现所有方式是不合适的。我们应该对其抽象，解耦各种实现方式。如下类图所示：
<div align="center">
   <img src="/doc/resource/bridge/消息通知结构设计.jpg" width="70%"/>
</div>


> 对于任意一种通知类型来说，都需要告知一个接收者的身份信息（identity），对于短信和机器人语音通话来说，是手机号码；而当通知方式是邮件时，则需要邮箱地址；站内消息则需要一个唯一的身份标识，以便明确告知系统这个通知应该由哪个用户接收。而不管何种通知方式，还需要一个通知的内容（content），比如邮件正文、短信模板、语音模板和站内信的正文。在通知方法中（doNotify），将根据具体的实现方式发送消息给接收的用户。

到此，我们已实现各个通知方式的统一和互相隔离，系统则根据需要构造对应的消息通知器，调用通知方法即可完成消息的推送。

然后让我们回归到需求中来，大多数时候我们总是希望用户能立即收到系统发起的消息，但并不是所有时候。比如上文中提到系统在下一个月的月初给用户推送统计报表邮件；再比如，为了保证用户的用眼健康，我们希望当用户连续使用 2 个小时的 APP 后，提示用户应该预防用眼过度，注意保护眼睛。我们给需求建模，总结出消息的触发方式至少应该有：

> - **立即推送类**：大多数需求期望用户能尽快收到来自系统的推送消息；
> - **延迟类**：该消息的推送将在一段时间之后触发；
> - **定时类**：该消息的推送将在未来的某一个具体的时间点触发；

当我们尝试把消息的触发方式加入到已有的结构中去时，我们总是最先考虑使用继承来进行扩展。把消息从触发机制和通知方式两个维度进行一对一的搭配，构建出独立的类对其表示。例如，对于下个月月初发送邮件给用户，我们使用定时触发 + 邮件推送（`TimerMailNotifer`）的组合方式表示；而对于提示用户注意用眼过度时，我们则应使用延迟触发 + 站内消息（`DelaySiteMessageNotifer`）的组合方式表示。对于系统来说，任意一种触发机制和任意一种通知方式的组合都存在可能。

|  | 立即推送类 | 延迟类 | 定时类 |
| --- | --- | --- | --- |
| 站内消息 | ImmediatelySiteMessage | DelaySiteMessage | TimerSiteMessage |
| 短信 | ImmediatelyShortMessage | DelayShortMessage | TimerShortMessage |
| 机器人语音通话 | ImmediatelyRobotCall | DelayRobotCall | TimerRobotCall |
| 邮件 | ImmediatelyMail | DelayMail | TimerMail |

这里描述的内容在数学上有一个专用的名词 —— 笛卡尔积。我们知道对于两个集合（A，B）来说，笛卡尔积是` A * B = {(A1,B1), (A1,B2), (A1, B3) ... (A1,Bn), (A2,B1) ... (Am,Bn)}`。对于上例中 4 种通知方式 和 3 种触发机制来说，笛卡尔积的数量为 4 * 3 = 12 种，也就是说，我们需要用 12 个类来对列举所有可能的组合。此时我们开始意识到系统可能存在着如下的问题：

1. 如果某天系统需要增加一种新的通知方式时，我们需要给新增的通知方式和任意一种触发机制都搭配一遍，增加触发机制亦是同理。如此反复，系统将越来越臃肿，类的数量将越来越多；
1. 更糟糕的是，多个类中充斥着重复类似的代码，因为他们有着相似的行为。比如对于延迟类短信、延迟类邮件、延迟类站内消息和延迟类机器人语音通话来说，他们有一部分代码是相似甚至雷同的，因为他们有着同样的行为 —— 将消息的推送工作延迟一段时间后再执行。

由此看来，采用继承来扩展多个维度并不明智。

# 二、解决方案

不知道你是否已经发现，问题的根本原因在于我们试图在一个对象中表述了两种行为。例如，对于定时发送类的邮件来说，其中包含了两个行为：定时发送、邮件。所以，用继承必然导致这个问题。

既然继承必然导致在一个对象中实现多个行为，那么理论上我们只要把这两个行为解耦到独立的对象中去，这个问题也就迎刃而解。在面向对象设计中，对一个对象进行扩展有两种实现方式，一种是使用继承，而另一种就是使用组合。巧的是，组合正好就能实现对多个行为的解耦。

采用组合进行解耦的基本思路是，将其中一个维度解放出来，并且将与自身无关的行为委托给另一个维度实现。用案例来说，对于定时发送和邮件两个行为，拆解到两个对象中处理，邮件对象只负责推送邮件给用户这一行为；而定时发送对象只负责在未来的某个时间触发一个任务，任务的执行则委托给与自身关联的邮件对象。整个过程如下图所示：
<div align="center">
   <img src="/doc/resource/bridge/消息发送过程.jpg" width="60%"/>
</div>

不管是触发机制，还是通知方式，都有多个具体的实现。所以，对上面的模型中的两个维度分别抽象，我们便能得到如下的类图结构：
<div align="center">
   <img src="/doc/resource/bridge/案例类图.jpg" width="90%"/>
</div>

在这个类图结构中，客户端直接向`AbstractTriggerExecutor`（触发机制执行器）提交请求，触发执行器依赖一个`AbstractNotifer`（通知发送器）。如何触发发送消息的逻辑在`AbstractTriggerExecutor#execute()`中实现，实现类决定在合适的时机触发推送任务；`AbstractTriggerExecutor`将推送任务委托给`AbstractNotifer`执行，具体的`AbstractNotifer`在各自的`doNotify()`中实现。而这个结构就是典型的桥模式。

# 三、案例实现
本篇仅仅是一个例子，并不打算真正实现短信、邮件发送等功能。有兴趣的朋友可自行实现更多细节。案例代码已附在文章结尾处，有需自取。

**（1）通知器抽象**
```java
public abstract class AbstractNotifer {

    /**
     * 身份标识：1.站内消息 -> 用户编号；2.邮件 -> 邮箱；3.短信/机器人语音通话 -> 电话号码
     */
    protected final String identity;

    /**
     * 通知内容
     */
    protected String content;


    public AbstractNotifer(String identity, String content) {
        this.identity = identity;
        this.content = content;
    }

    /**
     * 通知用户
     */
    protected abstract void doNotify();
}

```
**（2）实现多个通知方式**

**（2-1）邮件**
```java
public class MailNotifer extends AbstractNotifer {

    public MailNotifer(String identity, String content) {
        super(identity, content);
    }

    @Override
    protected void doNotify() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("       [{0}]发送邮件，【邮箱地址：{1}】，【内容：{2}】", time, super.identity, super.content));
    }
}
```
**（2-2）机器人语音通话**
```java
public class RobotCallNotifer extends AbstractNotifer {

    public RobotCallNotifer(String identity, String content) {
        super(identity, content);
    }

    @Override
    protected void doNotify() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("       [{0}]发起机器人语音通话，【手机号码：{1}】，【内容：{2}】", time, super.identity, super.content));
    }
}
```
**（2-3）短信**
```java
public class ShortMessageNotifer extends AbstractNotifer {

    public ShortMessageNotifer(String identity, String content) {
        super(identity, content);
    }

    @Override
    protected void doNotify() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("       [{0}]发送短信，【手机号码：{1}】，【内容：{2}】", time, super.identity, super.content));
    }
}
```
**（2-4）站内消息**
```java
public class SiteMessageNotifer extends AbstractNotifer {

    public SiteMessageNotifer(String identity, String content) {
        super(identity, content);
    }

    @Override
    protected void doNotify() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("       [{0}]发送站内消息，【用户编号：{1}】，【内容：{2}】", time, super.identity, super.content));
    }
}
```
**（3）触发机制执行器抽象**
```java
public abstract class AbstractTriggerExecutor {

    /**
     * 通知器
     */
    protected final AbstractNotifer notifer;


    public AbstractTriggerExecutor(AbstractNotifer handler) {
        this.notifer = handler;
    }

    /**
     * 执行
     * @throws InterruptedException 中断异常
     */
    protected abstract void execute() throws InterruptedException;

}
```
**（4）实现多个触发机制**

**（4-1）立即执行的执行器**
```java
public class ImmediatelyExecutor extends AbstractTriggerExecutor {

    public ImmediatelyExecutor(AbstractNotifer handler) {
        super(handler);
    }

    @Override
    protected void execute() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("    [{0}]已提交通知到立即执行处理器...", time));
        super.notifer.doNotify();
    }
}
```
**（4-2）延时一段时间执行的执行器**
```java
public class DelayExecutor extends AbstractTriggerExecutor {

    /**
     * 延迟秒数
     */
    private final int delaySeconds;

    public DelayExecutor(AbstractNotifer handler, int delaySeconds) {
        super(handler);
        this.delaySeconds = delaySeconds;
    }

    @Override
    protected void execute() throws InterruptedException {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("    [{0}]已提交通知到延迟执行处理器...", time));
        Thread.sleep(delaySeconds * 1000L);
        super.notifer.doNotify();
    }
}
```
**（4-3）定时执行的执行器**
```java
public class TimerExecutor extends AbstractTriggerExecutor {

    /**
     * 定时表达式
     */
    private final String express;

    public TimerExecutor(AbstractNotifer handler, String express) {
        super(handler);
        this.express = express;
    }

    @Override
    protected void execute() throws InterruptedException {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(MessageFormat.format("    [{0}]已提交通知到定时执行处理器，定时表达式为【{1}】...", time, express));
        // todo: 在此处实现定时触发的机制
        Thread.sleep(2000);
        // 模拟系统统计报表
        int num = 2;
        double total = 38900.0;
        super.notifer.content = MessageFormat.format(super.notifer.content, num, total);
        super.notifer.doNotify();
    }
}
```
**（5）客户端**

**（5-1）Client**
```java
public class Client {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("|==> Start ---------------------------------------------------------------------------------------|");
        // 立即发送站内消息
        AbstractNotifer notifer1 = new SiteMessageNotifer("coder", "您的物流已更新，详情请查看：https://gitee.com/ry_always/DesignPatterns");
        AbstractTriggerExecutor executor1 = new ImmediatelyExecutor(notifer1);
        executor1.execute();
        // 3s后发起语音通话
        AbstractNotifer notifer2 = new ShortMessageNotifer("18890907878", "您已连续浏览 2 小时，请注意防护用眼过度！");
        AbstractTriggerExecutor executor2 = new DelayExecutor(notifer2, 3);
        executor2.execute();
        // 下月初发送统计邮件
        AbstractNotifer notifer3 = new MailNotifer("uyu-90@hotel.com", "这个月您一共买了 {0} 件衣服，共计消费 {1} 元！");
        AbstractTriggerExecutor executor3 = new TimerExecutor(notifer3, "0 0 0 0 1/1 ?");
        executor3.execute();
    }
}
```
**（5-2）运行结果**
```text
|==> Start ---------------------------------------------------------------------------------------|
    [15:10:08]已提交通知到立即执行处理器...
       [15:10:08]发送站内消息，【用户编号：coder】，【内容：您的物流已更新，详情请查看：https://gitee.com/ry_always/DesignPatterns】
    [15:10:08]已提交通知到延迟执行处理器...
       [15:10:11]发送短信，【手机号码：18890907878】，【内容：您已连续浏览 2 小时，请注意防护用眼过度！】
    [15:10:11]已提交通知到定时执行处理器，定时表达式为【0 0 0 0 1/1 ?】...
       [15:10:13]发送邮件，【邮箱地址：uyu-90@hotel.com】，【内容：这个月您一共买了 2 件衣服，共计消费 38,900 元！】
```

# 四、方案回顾
## 3.1 解决了类数量的几何式增长

回顾我们是如何一步一步的将系统构建成现在的模样：我们发现使用继承在解决对象的多维度扩展时所表现的能力让人失望，它带来了类的数量呈现几何式增长的问题，同时也带来了代码重复的问题。进而，我们使用组合来替代继承，利用组合的委托机制，加上对多个维度的拆解，轻松的实现在不同维度中扩展各自维度的行为。

> 例如，现在需要增加一种触发机制，这种触发机制是给定一个条件表达式，对这个条件进行监视，当条件为真时，触发推送通知。在现在的架构下，我们只需新增一个`AbstractTriggerExecutor`的实现类，并在`execute()`方法中实现这个逻辑即可。再也不用像使用继承一样，在一个维度上新增一个实现，不得不在另外维度上为每一个都提供一个搭配。

在采取继承时，我们为了描述 4 个通知方式和 3 个触发机制的搭配一共需要 4 * 3 = 12 个类；然而，当我们采用组合后，我们仅仅只需要 4 + 3 = 7 个类。这有效的解决了类的增量问题。

## 3.2 解决了重复的代码
同时，我们在对多个维度进行拆解的时候，顺便也解决了代码重复的问题。这是令人惊喜的，在使用继承来扩展多个维度时，我们不得不将同样的逻辑在不同的类中进行实现，比如延迟短信和延迟邮件，他们是不同的类，但延迟的机制是一致的，所以这两个类中必然有部分代码会出现重复。而现在，我们的每一个都只存在一个行为，自然也就不存在代码重复的问题了。

## 3.3 让各个维度独立的扩展

事实上，桥模式带来的好处不止这两点，他还有一个隐藏的优点，并且这个优点才真正代表桥模式的核心。这个优点是：使用桥模式让原本混杂在一起的各个维度，现在可以独立的扩展了。

> 想象一下，如果我们新增一种触发机制，对整个结构来说，哪些类应该作出调整呢？是的，除了新增一个`AbstractTriggerExecutor`的实现类之外，在两个维度上没有任何需要调整的。那么，换成通知方式呢？是的，与前面的表现一致。这说明，触发机制和通知方式已经分离开了，他们可以在各自的维度上自由变化。对于触发机制的改变不会影响到通知方式，反之亦然。

# 五、桥模式
有了前面的铺垫，桥模式的理解将变得轻松，接下来，我们将对桥模式进行分析。

## 5.1 意图
> **将抽象部分与它的实现部分分离，使它们都可以独立地变化。**

桥模式之所以被认为是最难理解的设计模式，我觉得很大程度上在于它有一个相当难以理解的意图描述。抽象？实现？我在第一次看到这个意图时，也是不知所云。当然甚至觉得桥模式可能是一种修改类的字节码之类的技术。。。
> - a） 那么这里的抽象和实现到底指的是什么呢？
> 
事实上，这里的抽象和 Java 中的抽象类和接口完全不是一回事。这里的抽象只是一个概念，它泛指架构中位于上层的那部分，在上面的例子中就是指触发机制这一维度。
> - b） 那么为什么要用“抽象部分”一词来描述触发机制这个维度呢？
> 
前面说过，对于消息通知具体是如何实现的，触发机制这个维度 （`AbstractTriggerExecutor`）并不负责实现，而是将这一行为委托给相对底层的通知方式维度（`AbstractNotifer`）实现。也就是说触发机制的维度不负责实现如何通知，从这个角度上来说，触发机制的维度是抽象的部分，而通知方式的维度才是实现部分。

在前面的解决方案总结中，我们也解释了桥模式为什么能让各个维度独立扩展和变化，这里就不再重复叙述了。总结来说桥模式的意图是：分离抽象部分（触发机制维度）和实现部分（通知方式维度），使得他们各自扩展，独立变化。

## 5.2 类图分析
<div align="center">
   <img src="/doc/resource/bridge/桥.png" width="40%"/>
</div>

桥模式的得名十有八九来源于它的结构，因为桥模式的结构就像是一座桥，搭建在两个维度之间。结构如下类图所示：
<div align="center">
   <img src="/doc/resource/bridge/经典桥模式类图.jpg" width="80%"/>
</div>

桥模式包含的角色有如下：

- **Abstraction**：定义抽象的接口，维护了一个 Implementor 类型对象的引用，便于在合适的时候将部分工作委托给这个引用的对象；
- **RefinedAbstraction**： 扩充由 Abstraction 定义的接口；  
- **Implementor**：定义实现类的接口，一般来说，Implementor 接口只提供基础的操作，而 Abstraction 则定义了基于这些基础操作所衍生的更高层次的操作；
- **ConcreteImplementor**：实现类的接口的具体实现。

# 六、深入
## 6.1 适用场景

**（1）从多个维度扩展一个对象**

前面说了，当我们需要从多个维度对一个对象进行扩展时，我们可以使用桥模式来让各个维度分离，进而实现各自独立的变化。

**（2）在运行时切换实现**

桥模式的特点之一是实现可以在运行时动态切换实现，只需要替换掉 Abstraction 依赖的实现对象即可。

## 6.2 使用小技巧

**（1）用合适的方式创建合适的  Implementor**

我们可以灵活的创建 Abstraction 所依赖的 Implementor。例如

1. 将该工作交给客户端，由客户端通过 Abstraction 的构造函数传入依赖。
1. 在 Abstraction 的构造函数中提供一个缺省的 Implementor，如果用户不指定依赖，则使用缺省的 Implementor 依赖。
1. 通过参数的方式决定，就像静态工厂一样。例如，给每一个 Implementor 提供一个与之对应的 key，通过传入的 key 决定具体创建哪一个 Implementor，当然，这种方式有一个前提：Abstraction 必须知道所有的 Implementor。

# 附录
[回到主页](/README.md)    [案例代码](/src/main/java/com/aoligei/structural/bridge)