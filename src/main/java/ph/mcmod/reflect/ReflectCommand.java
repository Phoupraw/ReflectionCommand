package ph.mcmod.reflect;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public class ReflectCommand {
	/**
	 * 该指令的名字
	 */
	public static final String NAME = "reflect";
	/**
	 * 用于构造{@link CommandSyntaxException}
	 */
	protected static final CommandExceptionType COMMAND_EXCEPTION_TYPE = new CommandExceptionType() {};
	/**
	 * 储存变量
	 */
	private static VariableMap variableMap = new VariableMap();
	
	/**
	 * 加载类，无它用
	 */
	public static void loadClass() {}
	
	/**
	 * 赋值，将{@link Assign#SOURCE}的值赋给{@link Assign#TARGET}
	 */
	public static class Assign {
		/**
		 * 该子命令的名称
		 */
		public static final String NAME = "assign";
		/**
		 * 第一个参数的名称，被赋值的变量名
		 */
		public static final String TARGET = "target";
		/**
		 * 第二个参数的名称，源的变量名
		 */
		public static final String SOURCE = "source";
		
		/**
		 * @param context 指令环境
		 *
		 * @return 如果成功赋值，返回1；否则抛出异常
		 *
		 * @throws CommandSyntaxException 如果{@link Assign#TARGET}={@code null}，则抛出
		 */
		public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			final String targetName = context.getArgument(TARGET, String.class);
			if (VariableMap.NULL.equals(targetName))
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new TranslatableText("command.reflect.assign.assign_null"));
			final String sourceName = context.getArgument(SOURCE, String.class);
			variableMap.assign(targetName, sourceName);
			context.getSource().sendFeedback(new TranslatableText("command.reflect.assign.feedback", sourceName, targetName, variableMap.getValue(sourceName)), true);
			return 1;
		}
		
		/**
		 * 注册子命令
		 *
		 * @param builder 参数建造器
		 */
		public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(Assign.NAME)
			  .then(CommandManager.argument(Assign.TARGET, StringArgumentType.string())
				.then(CommandManager.argument(Assign.SOURCE, StringArgumentType.string())
				  .executes(Assign::run))));
		}
		
	}
	
	public static class Literal {
		public static final String NAME = "literal";
		public static final String TEXT = "text";
		/**
		 * 一共有9种字面量，所以有9条子命令，每条子命令都有其各自的参数类型；其中{@link char}被视为无符号整数类型
		 */
		public static final Map<String, Pair<Class<?>, ArgumentType<?>>> LITERALS = ImmutableMap.<String, Pair<Class<?>, ArgumentType<?>>>builder()
		  .put("byte", new Pair<>(Byte.class, reader -> (byte) (int) IntegerArgumentType.integer(Byte.MIN_VALUE, Byte.MAX_VALUE).parse(reader)))
		  .put("short", new Pair<>(Short.class, reader -> (short) (int) IntegerArgumentType.integer(Short.MIN_VALUE, Short.MAX_VALUE).parse(reader)))
		  .put("int", new Pair<>(Integer.class, IntegerArgumentType.integer()))
		  .put("long", new Pair<>(Long.class, LongArgumentType.longArg()))
		  .put("float", new Pair<>(Float.class, FloatArgumentType.floatArg()))
		  .put("double", new Pair<>(Double.class, DoubleArgumentType.doubleArg()))
		  .put("char", new Pair<>(Character.class, reader -> (char) (int) IntegerArgumentType.integer(Character.MIN_VALUE, Character.MAX_VALUE).parse(reader)))
		  .put("boolean", new Pair<>(Boolean.class, BoolArgumentType.bool()))
		  .put("string", new Pair<>(String.class, StringArgumentType.greedyString()))
		  .build();
		
		public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
			LiteralArgumentBuilder<ServerCommandSource> l0 = CommandManager.literal(Literal.NAME);
			for (Map.Entry<String, Pair<Class<?>, ArgumentType<?>>> entry : LITERALS.entrySet()) {
				l0.then((CommandManager.literal(entry.getKey())
				  .then(CommandManager.argument(Literal.TEXT, entry.getValue().getSecond())
					.executes(context -> {
						Object object = context.getArgument(TEXT, entry.getValue().getFirst());
						variableMap.setTemp(object);
						context.getSource().sendFeedback(new TranslatableText("command.reflect.literal.feedback", object), false);
						return 1;
					}))));
			}
			builder.then(l0);
		}
		
	}
	
	public static class Print {
		public static final String NAME = "print";
		public static final String VAR = "var";
		
		public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			final String name = context.getArgument(VAR, String.class);
			Object object = variableMap.getValueOrThrow(name);
			context.getSource().sendFeedback(new TranslatableText("command.reflect.print.feedback", object == null ? "null" : object.getClass(), object), false);
			return 1;
		}
		
		public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(Print.NAME)
			  .then(CommandManager.argument(Print.VAR, StringArgumentType.string())
				.executes(Print::run)));
		}
	}
	
	public static class GetField {
		public static final String NAME = "field";
		public static final String CLASS = GetMethod.CLASS;
		public static final String FIELD = "field";
		
		public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			final String className = context.getArgument(CLASS, String.class);
			final String fieldName = context.getArgument(FIELD, String.class);
			try {
				Field field = getAnyClass(className).getField(fieldName);
				variableMap.setTemp(field);
				context.getSource().sendFeedback(new TranslatableText("command.reflect.got_field", fieldName), true);
				return 1;
			} catch (Throwable e) {
				if (e instanceof CommandSyntaxException)
					throw (CommandSyntaxException) e;
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new LiteralText(e.toString()));
			}
		}
		
		public static final Map<String, Class<?>> PRIMITIVES = ImmutableMap.<String, Class<?>>builder()
		  .put("byte", byte.class)
		  .put("short", short.class)
		  .put("int", int.class)
		  .put("long", long.class)
		  .put("float", float.class)
		  .put("double", double.class)
		  .put("boolean", boolean.class)
		  .put("char", char.class)
		  .put("void", void.class)
		  .build();
		
		public static Class<?> getAnyClass(String name) throws CommandSyntaxException {
			if (PRIMITIVES.containsKey(name))
				return PRIMITIVES.get(name);
			//			if (name.charAt(name.length() - 1) == '+') {
			//				Class<?> clazz = Array.newInstance(getAnyClass(name.substring(0, name.length() - 1)), 0).getClass();
			//				System.out.println(Arrays.toString(clazz.getConstructors()));
			//				return clazz;
			//			}
			try {
				return Class.forName(name);
			} catch (Throwable e) {
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new LiteralText(e.toString()));
			}
		}
		
		public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(NAME)
			  .then(CommandManager.argument(CLASS, StringArgumentType.string())
				.then(CommandManager.argument(FIELD, StringArgumentType.word())
				  .executes(GetField::run))));
		}
		
	}
	
	public static class Get {
		public static final String NAME = "get";
		public static final String SUBJECT = Invoke.SUBJECT;
		
		public static int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			final Object temp = variableMap.getTemp();
			if (!(temp instanceof Field))
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new TranslatableText("command.reflect.temp_not_field", temp, temp == null ? "null" : temp.getClass()));
			final Field field = (Field) temp;
			final String subjectName = context.getArgument(SUBJECT, String.class);
			final Object subject = variableMap.getValueOrThrow(subjectName);
			try {
				final Object value = field.get(subject);
				variableMap.setTemp(value);
				context.getSource().sendFeedback(new TranslatableText("command.reflect.got", field, value), true);
				return 1;
			} catch (Throwable e) {
				if (e instanceof CommandSyntaxException)
					throw (CommandSyntaxException) e;
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new LiteralText(e.toString()));
			}
		}
		
		public static void register(final LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(NAME)
			  .then(CommandManager.argument(SUBJECT, StringArgumentType.word())
				.executes(Get::run)));
		}
	}
	
	public static class GetMethod {
		public static final String NAME = "method";
		public static final String CLASS = "class";
		public static final String METHOD = "method";
		public static final String PARAS_CLASSES = "paras_classes";
		
		public static int run(final CommandContext<ServerCommandSource> context, final boolean nonePara) throws CommandSyntaxException {
			final String className = context.getArgument(CLASS, String.class);
			final String methodName = context.getArgument(METHOD, String.class);
			final String[] parasNames = nonePara ? new String[0] : context.getArgument(PARAS_CLASSES, String.class).split(" ");
			try {
				final Class<?>[] parasClasses = new Class[parasNames.length];
				for (int i = 0; i < parasClasses.length; i++) {
					if ("double".equals(parasNames[i]))
						parasClasses[i] = double.class;
					else
						parasClasses[i] = GetField.getAnyClass(parasNames[i]);
				}
				final Class<?> clazz = GetField.getAnyClass(className);
				final Executable executable;
				if ("new".equals(methodName) || "<init>".equals(methodName)) {
					executable = clazz.getConstructor(parasClasses);
				} else {
					executable = clazz.getMethod(methodName, parasClasses);
				}
				variableMap.setTemp(executable);
				context.getSource().sendFeedback(new TranslatableText("command.reflect.got_method", executable), true);
				return 1;
			} catch (Throwable e) {
				if (e instanceof CommandSyntaxException)
					throw (CommandSyntaxException) e;
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new LiteralText(e.toString()));
			}
		}
		
		public static void register(final LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(NAME)
			  .then(CommandManager.argument(CLASS, StringArgumentType.string())
				.then(CommandManager.argument(METHOD, StringArgumentType.word())
				  .executes(context -> run(context, true))
				  .then(CommandManager.argument(PARAS_CLASSES, StringArgumentType.greedyString())
					.executes(context -> run(context, false))))));
		}
		
	}
	
	public static class Invoke {
		public static final String NAME = "invoke";
		public static final String SUBJECT = "subject";
		public static final String PARAS = "paras";
		
		public static int run(final CommandContext<ServerCommandSource> context, final boolean nonePara) throws CommandSyntaxException {
			final Object temp = variableMap.getTemp();
			if (!(temp instanceof Executable))
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new TranslatableText("command.reflect.temp_not_method_or_constructor", temp, temp == null ? "null" : temp.getClass()));
			final Executable method = (Executable) temp;
			final String subjectName = context.getArgument(SUBJECT, String.class);
			final String[] parasNames = nonePara ? new String[0] : context.getArgument(PARAS, String.class).split(" ");
			final Object object = variableMap.getValueOrThrow(subjectName);
			final Object[] paras = new Object[parasNames.length];
			for (int i = 0; i < paras.length; i++)
				paras[i] = variableMap.getValueOrThrow(parasNames[i]);
			final Pair<Object, Class<?>> returnInfo = execute(method, object, paras);
			final Object returnValue = returnInfo.getFirst();
			final String returnString;
			if (returnInfo.getSecond() == Void.TYPE) {
				returnString = "void";
			} else {
				returnString = Objects.toString(returnValue);
				variableMap.setTemp(returnValue);
			}
			context.getSource().sendFeedback(new TranslatableText("command.reflect.invoked", method, returnString), true);
			return 1;
		}
		
		public static Pair<Object, Class<?>> execute(Executable executable, Object subject, Object... paras) throws CommandSyntaxException {
			try {
				if (executable instanceof Method) {
					Method method = (Method) executable;
					Object returnValue = method.invoke(subject, paras);
					Class<?> returnClass = method.getReturnType();
					return new Pair<>(returnValue, returnClass);
				}
				if (executable instanceof Constructor<?>) {
					Constructor<?> constructor = (Constructor<?>) executable;
					Object instance = constructor.newInstance(paras);
					return new Pair<>(instance, constructor.getDeclaringClass());
				}
				throw new IllegalArgumentException("executable既不是Method也不是Constructor");
			} catch (Throwable e) {
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new LiteralText(e.toString()));
			}
		}
		
		public static void register(final LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(NAME)
			  .then(CommandManager.argument(SUBJECT, StringArgumentType.word())
				.executes(context -> run(context, true))
				.then(CommandManager.argument(PARAS, StringArgumentType.greedyString())
				  .executes(context -> run(context, false)))));
		}
	}
	
	public static class RunCommand {
		public static final String NAME = "runcommand";
		public static final String COMMAND = "command";
		
		public static int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			final String stringName = context.getArgument(COMMAND, String.class);
			final String command = Objects.toString(variableMap.getValueOrThrow(stringName));
			return context.getSource().getMinecraftServer().getCommandManager().execute(context.getSource(), command);
		}
		
		public static void register(final LiteralArgumentBuilder<ServerCommandSource> builder) {
			builder.then(CommandManager.literal(NAME)
			  .then(CommandManager.argument(COMMAND, StringArgumentType.string())
				.executes(RunCommand::run)));
		}
		
	}
	
	public static class ArrayOp {//TODO
		public static final String NAME = "array";
		
		public static class New {
			public static final String NAME = "new";
			public static final String CLASS_NAME = "class_name";
			public static final String DIMENSIONS = "dimensions";
			public static int run( CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
				String className=context.getArgument(CLASS_NAME,String.class);
				String stringDimensions=context.getArgument(DIMENSIONS,String.class);
				return 1;
			}
			public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
				builder.then(CommandManager.literal(NAME)
				  .then(CommandManager.argument(CLASS_NAME, StringArgumentType.string())
					.then(CommandManager.argument(DIMENSIONS, IntegerArgumentType.integer(0, Integer.MAX_VALUE))
					  .executes(context -> 1))));
			}
		}
		
		public static void register(LiteralArgumentBuilder<ServerCommandSource> builder) {
			ArgumentBuilder<ServerCommandSource, LiteralArgumentBuilder<ServerCommandSource>> builder1 = CommandManager.literal(NAME);
			builder.then(builder1);
		}
	}
	
	static {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal(NAME).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2));
			Assign.register(builder);
			Literal.register(builder);
			Print.register(builder);
			GetField.register(builder);
			Get.register(builder);
			GetMethod.register(builder);
			Invoke.register(builder);
			RunCommand.register(builder);
			//清除所有变量
			builder.then(CommandManager.literal("clearvars")
			  .executes(context -> {
				  variableMap = new VariableMap();
				  context.getSource().sendFeedback(new TranslatableText("command.reflect.clearvars.feedback"), true);
				  return 1;
			  }));
			dispatcher.register(builder);
		});
		
	}
	
	public static class VariableMap implements Iterable<Map.Entry<String, Object>> {
		/**
		 * 临时变量名，用于储存新创建的对象、新解析的字面量、访问得到的字段、方法的返回值
		 */
		public static final String TEMP = "_temp";
		/**
		 * 代表字面量{@literal null}
		 */
		public static final String NULL = "null";
		/**
		 * 用这个赋值，可以把所有的变量赋值
		 */
		public static final String ALL = "_all";
		private final Map<String, Object> map;
		
		public VariableMap() {
			map = new HashMap<>();
		}
		
		@SuppressWarnings("unused")
		public VariableMap(VariableMap variableMap) {
			map = new HashMap<>(variableMap.map);
		}
		
		public boolean containsName(String name) {
			return map.containsKey(name);
		}
		
		/**
		 * 设置某个变量的值
		 *
		 * @param name 变量名
		 * @param value 值
		 *
		 * @throws CommandSyntaxException 如果name=null，则抛出
		 */
		public void setValue(String name, Object value) throws CommandSyntaxException {
			if (NULL.equals(name))
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new TranslatableText("command.reflect.assign.assign_null"));
			if (ALL.equals(name)) {
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if (ALL.equals(entry.getKey()) || NULL.equals(entry.getKey()))
						continue;
					entry.setValue(value);
				}
			} else {
				map.put(name, value);
			}
		}
		
		/**
		 * 获取变量的值
		 *
		 * @param name 变量名
		 *
		 * @return 值；如果不存在，返回null
		 */
		@Nullable
		public Object getValue(String name) {
			if (NULL.equals(name))
				return null;
			if (ALL.equals(name))
				return map;
			return map.get(name);
		}
		
		/**
		 * 获取某个变量的类型
		 *
		 * @param name 变量名
		 *
		 * @return 类型；如果不存在或值为null，则返回null
		 */
		@Nullable
		@SuppressWarnings("unused")
		public Class<?> getClass(String name) {
			Object object = getValue(name);
			if (object != null)
				return object.getClass();
			return null;
		}
		
		/**
		 * 赋值
		 *
		 * @param target 被赋值的变量名
		 * @param source 用于赋值的变量名
		 *
		 * @throws CommandSyntaxException 如果target=null，则抛出
		 */
		public void assign(String target, String source) throws CommandSyntaxException {
			setValue(target, getValueOrThrow(source));
		}
		
		/**
		 * 设置临时变量
		 *
		 * @param value 值
		 *
		 * @throws CommandSyntaxException 如果{@link VariableMap#TEMP}=={@link VariableMap#NULL}，则抛出；正常情况不抛出
		 */
		public void setTemp(Object value) throws CommandSyntaxException {
			setValue(TEMP, value);
		}
		
		public Object getTemp() {return getValue(TEMP);}
		
		/**
		 * 获取变量的值
		 *
		 * @param name 变量名
		 *
		 * @return 值
		 *
		 * @throws CommandSyntaxException 如果不存在该变量，则抛出
		 */
		public Object getValueOrThrow(String name) throws CommandSyntaxException {
			if (!containsName(name) && !NULL.equals(name) && !ALL.equals(name))
				throw new CommandSyntaxException(COMMAND_EXCEPTION_TYPE, new TranslatableText("command.reflect.no_such_var", name));
			return getValue(name);
		}
		
		@NotNull
		@Override
		public Iterator<Map.Entry<String, Object>> iterator() {
			return map.entrySet().iterator();
		}
	}
}
