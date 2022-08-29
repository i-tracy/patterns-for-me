## <center> 行为型 - 备忘录（Memento）设计模式
---

> **在不破坏封装性的前提下，捕获一个对象的内部状态，并在该对象之外保存这个状态。这样以后就可以将该对象恢复到原先保存的状态。**

《Design patterns- Elements of reusable object-oriented software》一书中，关于备忘录模式的描述如上。从备忘录模的意图来看，不破坏封装性是前提，在合适的时机记录下对象的状态是目的。既然我们已经知道了备忘录的意图，那么试试按照意图来对其进行实现。

# 一、案例引入
在备忘录模式的意图中，已经明确提到：捕捉对象的状态，并在对象之外保存这个状态。这一点足以说明，完成状态存储的工作至少需要两个对象的协作才能完成。事实上，即便意图中没有提及，你也应该意识到这一点。一个对象无法同时表示两个不同的状态**，**就像一块石头不能同时位于两个不同的位置。

> 相信大家都玩过生存类型的游戏，但是这种类型的游戏玩起来需要特别小心，要是一不小心死亡，之前做的努力就全都白费。但是，这类游戏大都提供了一个存档的功能，就是专门为玩家解决死亡后需要从头开始的窘境。我们就以生存游戏为案例，来演示备忘录模式。

# 二、案例实现
## （方案一）克隆对象保存状态
在 Java 中，我们可以使用克隆的方式来轻松获取一个对象现场状态，需要注意的是，根据需求灵活的选择对某个属性进行深拷贝或者浅拷贝。除此之外，我们还需要一个容器，用来存储多个存档的引用，并且按照顺序对多个历史存档进行组织，这样我们就能让游戏方便的回到上一个存档点。
<div align="center">
   <img src="/doc/resource/memento/方案一类图.jpg" width="80%"/>
</div>

这个设计完全满足需求，我们不仅记录了对象（Game）的状态，并且在对象之外（SavepointHistory）中保存了该对象。用户可以选择在合适的时间存档游戏（store），并且可以在任意时间恢复游戏到上一个存档点的状态。

## （方案二）抽取状态对象
上面的设计的确能够很好的工作，并且，它满足备忘录模式的要求，属于备忘录模式的一种实现。但该方式在有些时候是有局限性的：因为对象的部分属性对恢复后的状态来说无关紧要，或者部分属性是不可变的。

> 方案一中，我们在游戏存档时一共存储了 4 个属性，分别是金币、游戏的背景音乐、背景音乐的播放进度、当前游戏的剩余血条。金币、背景音乐和当前游戏的血条对游戏有着至关重要的作用，所以必须记录状态；但背景音乐的播放进度则显得无足轻重，对于任一时刻的游戏来说，只要是同一首 BGM，则并不需要某一个时候必须同步播放该 BGM 到某个时间刻度。

对于无关紧要的属性和不可变的属性（无论哪个存档，该属性都是一样的，比如，当前游戏对局所属的窗体对象在当前进程运行过程中始终不会改变），存档时保存他们的值是不合适的，因为没有必要。如果单个存档的占用相当大，且存档的数量也越来越多，那么舍弃那些没有必要的状态对于游戏体验的提升可能是巨大的。

> 有一种可行的解决办法是修改克隆的方法，使得在进行对象克隆时，仅考虑那些需要进行存档的属性。但这种方案存在风险，如果有其他的类正依赖克隆方法，且希望克隆方法的行为和当前的行为不一致时，将导致其他依赖的地方出现问题，所以需要谨慎考虑。这种实现，这里就不再介绍了，和上面的结构一致，仅仅是修改克隆方法为只处理那些必要的状态属性。

这里，我们提供另外一种解决思路。既然存档对象的状态仅是原始对象状态的一部分，那么我们可以为原始对象中需要的部分状态属性提供一个全新的对象。按照这个思路，我们对方案一进行修改：
<div align="center">
   <img src="/doc/resource/memento/方案二类图.jpg" width="90%"/>
</div>

可以看到，状态对象中，只有我们关心的属性了，我们成功的根据需求场景对设计做出了灵活调整。单独封装类来屏蔽掉不关心的职责，也让代码在可维护性和可阅读性上有了更好的表现。看起来，一切好像都很完美。

> 这份代码在实际开发中不断的迭代，你和你的同事同时在这个基础上对功能进行完善、对需求进行扩展。直到有一天，用户反映了一个问题：对游戏存档后，无法恢复到存档前的状态，表现是：恢复后金币是 0。你开始排查问题，最终你找到了真相，你的同事在代码中不小心调用了 Savepoint.setMoney() 方法，正是这个调用让某个存档的状态发生了变化。你取消了这个调用，游戏恢复了正常。但你无法感到心安，因为你不知道哪天又会爆发同样的问题。

你开始意识到这个问题的根本原因不在错误的调用，而在于存档竟然给外部对象敞开了修改状态值的大门！**存档一旦建立，就意味着中途不能被任何其他对象所修改，甚至存档中包含的状态不应该被游戏对局之外的对象所访问**。但显然，当前的这个设计破坏了状态的封装性。

## （方案三）嵌套类保证状态的封装性
那么，是否有办法能让除了游戏对象之外的其他对象都无法访问存档点的状态属性吗？有，还不止一种。这里介绍一种使用比较多的解决办法 —— 内部类。将存档点类以内部类的方式定义在游戏类中，这样游戏类对象就可以轻易的访问存档点对象的任何属性。
<div align="center">
   <img src="/doc/resource/memento/方案三类图.jpg" width="80%"/>
</div>

如上图所示，深色部分表示他们共同定义在一个类中。在这次改动中，我们没有对他们的任何功能做出调整，但这个实现保证了除了游戏对象之外，存档点的状态属性不能被任何其他对象所访问。这很好的维护了存档点的封装性。

到此为止，我们已经提供了三种对于备忘录模式的不同实现。他们有着各自的特点，我们可以根据实际需求灵活选择，或者基于他们扩展更适合需求的方案。

# 三、代码附录
我们选择对方案三进行实现，代码如下。
**（1）游戏及存档状态**
```java
public class Game implements ActionListener {

    private int money;                                                      // 金币
    private String bgm;                                                     // 背景音乐
    private int bgmProgress;                                                // 背景音乐播放进度
    private int bloodBar;                                                   // 血条
    private final Timer timer = new Timer(1000, this);         // 计时器
    private final Random random = new Random();                             // 随机数生成器

    public Game(String bgm) {
        this.actionPerformed(null);
    }

    /**
     * 切换BGM
     * @param bgm bgm
     */
    public void switchBgm(String bgm) {
        // 从头开始播放音乐
        this.bgmProgress = 1;
        this.bgm = bgm;
    }


    public void showStatus() {
        String status = MessageFormat.format("      游戏状态为【金币：{0}，血条：{1}，BGM：{2}，BGM播放进度：{3}秒】",
                money, bloodBar, bgm, bgmProgress);
        System.out.println(status);
    }

    /**
     * 创建存档点
     * @return 存档点
     */
    public Savepoint createSavepoint() {
        System.out.println("    开始存档...");
        this.showStatus();
        return new Savepoint(this.money, this.bgm, this.bloodBar);
    }

    /**
     * 读档
     * @param point 存档点
     */
    public void restore(Savepoint point) {
        System.out.println("    恢复存档...");
        this.money = point.money;
        this.switchBgm(point.bgm);
        this.bloodBar = point.bloodBar;
        this.showStatus();
    }

    /**
     * 定时器计时结束时执行
     * @param e e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 每过一秒，金币+100，血条重置为1-100之间的随机数
        this.money += 100;
        this.bloodBar = random.nextInt(100);
        this.bgmProgress += 1;
        // 游戏开始，计时器每过 1s 打印一次游戏状态
        this.showStatus();
        // 计时开始
        timer.start();
    }

    /**
     * 存档状态
     */
    protected static class Savepoint {
        private final int money;
        private final String bgm;
        private final int bloodBar;
        public Savepoint(int money, String bgm, int bloodBar) {
            this.money = money;
            this.bgm = bgm;
            this.bloodBar = bloodBar;
        }
    }
}
```
**（2）历史存档点**
```java
public class SavepointHistory {

    /**
     * 当前游戏
     */
    private final Game currentGame;

    /**
     * 存档点容器
     */
    private final Stack<Game.Savepoint> savepointStack = new Stack<>();
    public SavepointHistory(Game currentGame) {
        this.currentGame = currentGame;
    }

    /**
     * 存档
     */
    public void store() {
        Game.Savepoint point = currentGame.createSavepoint();
        savepointStack.push(point);
    }

    /**
     * 读档
     */
    public void restore() {
        if (!savepointStack.isEmpty()) {
            Game.Savepoint point = savepointStack.pop();
            currentGame.restore(point);
        }
    }
}
```
**（3）客户端**

**（3-1）Client**
```java
public class Client {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("|==> Game Start -------------------------------------------------------|");
        // 创建一个游戏对局
        Game game = new Game("someone like you");
        // 游戏进行 2s
        Thread.sleep(2000);
        // 第一次存档
        SavepointHistory history = new SavepointHistory(game);
        history.store();
        // 换一首bgm
        game.switchBgm("my love");
        // 游戏进行 3s
        Thread.sleep(3000);
        // 第二次存档
        history.store();
        // 游戏进行 3s
        Thread.sleep(3000);

        // 第一次读档
        history.restore();
        // 游戏进行 2s
        Thread.sleep(2000);
        // 第二次读档
        history.restore();
        System.exit(0);
    }
}

```
**（3-2）运行结果**
```text
|==> Game Start -------------------------------------------------------|
      游戏状态为【金币：100，血条：40，BGM：null，BGM播放进度：1秒】
      游戏状态为【金币：200，血条：8，BGM：null，BGM播放进度：2秒】
    开始存档...
      游戏状态为【金币：200，血条：8，BGM：null，BGM播放进度：2秒】
      游戏状态为【金币：300，血条：23，BGM：my love，BGM播放进度：2秒】
      游戏状态为【金币：400，血条：66，BGM：my love，BGM播放进度：3秒】
      游戏状态为【金币：500，血条：8，BGM：my love，BGM播放进度：4秒】
    开始存档...
      游戏状态为【金币：500，血条：8，BGM：my love，BGM播放进度：4秒】
      游戏状态为【金币：600，血条：16，BGM：my love，BGM播放进度：5秒】
      游戏状态为【金币：700，血条：46，BGM：my love，BGM播放进度：6秒】
      游戏状态为【金币：800，血条：78，BGM：my love，BGM播放进度：7秒】
    恢复存档...
      游戏状态为【金币：500，血条：8，BGM：my love，BGM播放进度：1秒】
      游戏状态为【金币：600，血条：44，BGM：my love，BGM播放进度：2秒】
      游戏状态为【金币：700，血条：43，BGM：my love，BGM播放进度：3秒】
    恢复存档...
      游戏状态为【金币：200，血条：8，BGM：null，BGM播放进度：1秒】
```

# 四、备忘录模式
## 3.1 结构
备忘录模式的通用类图如下：
<div align="center">
   <img src="/doc/resource/memento/备忘录模式类图.jpg" width="60%"/>
</div>

备忘录模式的参与者有如下：

- **Memento**：备忘录，内部存储了原发器（Originator）在某个时刻的状态，但并不需要存储所有内部状态，方案三中的存档点就属于备忘录；
- **Originator**：原发器，它可以创建备忘录，以此来记录当前时刻的内部状态，如果需要可使用备忘录来恢复状态，方案三中的游戏就属于原发器；
- **Caretaker**：负责人，负责保存备忘录，如果需要，也可以保存多个备忘录，方案三中的历史存档点就属于负责人。

## 3.2 适用场景
必须保存一个对象在某一个时刻的 （部分）内部状态，这样以后需要时它才能恢复到先前的状态。我们不仅可以在需要使用“撤销”功能的地方使用备忘录模式，而且在处理事务（在事务处理过程中发生错误，需要回滚到事务开始之前的状态）的过程中也可使用该模式。

## 3.3 使用技巧

**（1）尽量保证备忘录内部状态的封装性**

正如我们在方案二到方案三过渡中描述，备忘录的内部状态仅仅只是用来恢复游戏的，所以我们应尽量保证除了游戏之外的对象不能访问到备忘录的内部状态。总之一句话，推荐使用方案三，而不是方案二。

**（2）增量式存储**

有些时候，我们可以使用增量式存储来减小备忘录的开销。比如，对于一个对象，它的每一次存档都仅仅只变化了一小部分的内部状态，且恢复的顺序为先备份的最后还原，那么我们就可以在每次状态发生改变时只存储相对于上一个存档点变化的那部分状态。例如，对一个数进行加减的回滚过程可能是这样的：

| **时刻** | **t1** | **t2** | **t3** | **t4** | **t5** | **t6** |
| --- | --- | --- | --- | --- | --- | --- |
| **状态值** | 初始值：0 | 3 | 8 | 6 | 8 | 3 |
| **存档内容** | 
 | +3 | +5 | -2 | 
 | 
 |
| **回滚内容** | 
 | 
 | 
 | 
 | +2 | +5 |

# 五、案例扩展
在命令模式（Command）那篇中，我们实现了一个小猫摘星星的游戏，游戏中后退一步的功能就使用了备忘录模式。关于该游戏的规则约定如下：

> a. 小猫朝着当前的方向移动，如果沿途遇到小星星，则摘取该星星；
> b. 星星被摘取后，将在游戏屏幕内随机生成一颗新的星星；
> c. 小猫碰到边界，游戏结束，否则，游戏一直进行下去；
> d. 游戏对局中提供对移动步数、星星得分的统计；
> e. 游戏对局中提供暂停/继续功能、提供转向功能（只允许水平向垂直转向，或者垂直向水平转向）、提供新开对局功能；
> f. 当游戏处于暂停或结束时，可使用后退一步功能恢复当前游戏到最近一次转向之前的状态；

该游戏的代码参见附录。游戏的运行效果如下所示：
<div align="center">
   <img src="/doc/resource/command/gaming.gif" width="70%"/>
</div>

# 附录
[回到主页](/README.md)    [案例代码](/src/main/java/com/aoligei/behavioral/memento)    [小猫摘星星代码](/src/main/java/com/aoligei/behavioral/command/game)