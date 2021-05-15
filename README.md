# 反射
增加了可以使用Java反射的命令：`reflect`

`reflect`有9个子命令：`assign`, `literal`, `print`, `field`, `get`, `method`, `invoke`, `runcommand`, `clearvar`
- `/reflect assign <target> <source>`
  - **赋值**：把`<source>`的值赋给`<target>`，如果`<target>`为`_all`，则将`<source>`的值赋给所有变量
  - 示例：`/reflect assign myVar _temp`该指令把`_temp`的值赋给`myVar`——相当于代码`myVar = _temp;`
- `/reflect literal <byte|short|int|long|float|double|char|boolean|string> <text>`
  - **字面量**：根据类型解析字面量，并把解析的结果储存到变量`_temp`，其中，`long`、`float`、`double`类型没有`L`、`F`、`D`的单位，`char`被视为无符号整数类型，`string`不带引号
  - 示例：`/reflect literal char 48`该指令将`48`当做无符号整数`char`解析，结果为`48`，即字符常量`'0'`
- `/reflect print <var>`
  - **打印**：将变量`<var>`打印到聊天栏或命令方块反馈栏中
  - 示例：`/reflect print _temp`该指令把`_temp`的类型和值打印到聊天栏或命令方块反馈栏中
- `/reflect field <class> <field>`
  - **字段**：获取`<class>`类的`<field>`字段（是一个java.lang.reflect.Field对象，不是这个字段的值），并储存到`_temp`变量
  - 示例：`/reflect field java.lang.Character MAX_VALUE`该指令获取了`java.lang.Character`的`MAX_VALUE`字段，作为一个`java.lang.reflect.Field`对象，储存到变量`_temp`——相当于代码`_temp=java.lang.Character.class.getFiled("MAX_VALUE");`
- `/reflect get <subject>`
  - **获取**：获取变量`<subject>`的储存在变量`_temp`的字段的值，并储存到变量`_temp`。这要求变量`_temp`必须是一个`java.lang.reflect.Filed`对象。如果是访问静态字段，`<subject>`填`null`
  - 示例：`/reflect get null`，假设当前`_temp`的值为上方的示例所获取的字段，则该指令以`null`为主体（因为是静态字段）获取了`java.lang.Character`的`MAX_VALUE`字段的值（`'\uFFFF'`)，作为一个`java.lang.Character`对象，储存到变量`_temp`——相当于代码`_temp=((Field)`_temp`).get(null);`
- `/reflect method <class> <method> [paras]`
  - **方法**：获取`<class>`类的名为`<method>`的参数列表为`[paras]`的方法或构造器（是一个`java.lang.reflect.Method`或`java.lang.reflect.Constructor`对象，不是调用这个方法或构造器）。如果`<method>`填`new`，则获取构造器，否则获取方法。`[paras]`填每个参数的类名，之间用单个空格分开。如果没有参数，则不要填`[paras]`
  - 示例：`/reflect method java.lang.String substring int int`，该指令获取了`java.lang.String`的`substring(int, int)`方法，作为一个`java.lang.reflect.Method`对象，储存到变量`_temp`——相当于代码`_temp=java.lang.String.class.getMethod("substring", int.class, int.class);`
- `/reflect invoke <subject> [paras]`
  - **调用**：以变量`<subject>`为主体调用储存在变量`_temp`的方法或构造器，传入变量`[paras]`参数，并把返回值储存到`_temp`。这要求`_temp`必须是一个`java.lang.reflect.Method`或`java.lang.reflect.Constructor`对象。如果是调用静态方法或构造器，则`<subject>`填`null`。`[paras]`填每个参数的变量名，之间用单个空格分开。如果没有参数，则不要填`[paras]`。
  - 示例：`/reflect invoke str start end`，假设当前`str`是一个`String`，`start`和`end`都是`Integer`，`_temp`的值为上方的示例所获取的方法，则该指令以`str`为主体调用了`substring`方法，传入参数`start`和`end`，并把返回值储存到变量`_temp`——相当于代码`_temp=((Method)_temp).invoke(str, start, end);`
- `/reflect runcommand <command>`
  - **运行指令**：对变量`<command>`调用`Objects.toString`方法，并将返回的字符串当做命令执行。执行参数（执行者、位置、角度、维度等）与该命令执行的相同。
  - 示例：`/reflect runcommand str`，假设当前`str`是`"tp ~ ~1 ~"`，则会将执行者传送到其上方一格——相当于指令`/tp ~ ~1 ~`;
- `/reflect clearvars`
  - **清除变量**：清除所有变量