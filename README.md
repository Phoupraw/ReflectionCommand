# 反射
增加了可以使用Java反射的命令：/reflect
/reflect有9个子命令：assign, literal, print, field, get, method, invoke, runcommand, clearvars
- /reflect assign <target> <source>
- - 赋值：把source的值赋给target，如果target为"_all"，则将source的值赋给所有变量
- - 示例：/reflect assign myVar _temp，该指令把_temp的值赋给myVar
- /reflect literal <byte|short|int|long|float|double|char|boolean|string> <text>
- - 字面量：根据类型解析字面量，并把解析的结果储存到变量_temp，其中，long、float、double类型没有L、F、D的单位，char被视为无符号整数类型，string不带引号
    示例：/reflect literal char 48，该指令将
- /reflect print <var>
- - 打印：将变量var打印到聊天栏或命令方块反馈栏中
- /reflect field <class> <field>
- - 字段：获取class类的field字段（是一个java.lang.reflect.Field对象，不是这个字段的值），并储存到_temp变量
- /reflect get <subject>
- - 获取：获取subject对象的储存在变量_temp的字段的值，并储存到变量_temp。这要求变量_temp必须是一个java.lang.reflect.Filed对象。要访问静态字段，subject填null
- /reflect method <class> <method> [paras]
- - 方法：获取class类的名为method的参数列表为paras的方法，paras填每个参数的类名，之间用单个空格分开，如果没有参数，则不要填paras