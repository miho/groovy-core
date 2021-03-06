/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.util.logging;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import org.codehaus.groovy.transform.LogASTTransformation;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This local transform adds a logging ability to your program using
 * Log4j logging. Every method call on a unbound variable named <i>log</i>
 * will be mapped to a call to the logger. For this a <i>log</i> field will be
 * inserted in the class. If the field already exists the usage of this transform
 * will cause a compilation error. The method name will be used to determine
 * what to call on the logger.
 * <pre>
 * log.name(exp)
 * </pre>is mapped to
 * <pre>
 * if (log.isNameEnabled() {
 *    log.name(exp)
 * }</pre>
 * Here name is a place holder for info, debug, warning, error, etc.
 * If the expression exp is a constant or only a variable access the method call will
 * not be transformed. But this will still cause a call on the injected logger.
 *
 * @author Hamlet D'Arcy
 * @author Tomek Bujok
 * @since 1.8.0
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.LogASTTransformation")
public @interface Log4j {
    String value() default "log";
    Class<? extends LogASTTransformation.LoggingStrategy> loggingStrategy() default Log4jLoggingStrategy.class;

    public static class Log4jLoggingStrategy extends LogASTTransformation.AbstractLoggingStrategy {
        private static final String LOGGER_NAME = "org.apache.log4j.Logger";
        private static final String PRIORITY_NAME = "org.apache.log4j.Priority";

        protected Log4jLoggingStrategy(final GroovyClassLoader loader) {
            super(loader);
        }

        public FieldNode addLoggerFieldToClass(ClassNode classNode, String logFieldName) {
            return classNode.addField(logFieldName,
                    Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE,
                    classNode(LOGGER_NAME),
                    new MethodCallExpression(
                            new ClassExpression(classNode(LOGGER_NAME)),
                            "getLogger",
                            new ClassExpression(classNode)));
        }

        public boolean isLoggingMethod(String methodName) {
            return methodName.matches("fatal|error|warn|info|debug|trace");
        }

        public Expression wrapLoggingMethodCall(Expression logVariable, String methodName, Expression originalExpression) {
            final MethodCallExpression condition;
            if (!"trace".equals(methodName)) {
                AttributeExpression logLevelExpression = new AttributeExpression(
                        new ClassExpression(classNode(PRIORITY_NAME)),
                        new ConstantExpression(methodName.toUpperCase()));
                ArgumentListExpression args = new ArgumentListExpression();
                args.addExpression(logLevelExpression);
                condition = new MethodCallExpression(logVariable, "isEnabledFor", args);
            } else {
                // log4j api is inconsistent, so trace requires special handling
                condition = new MethodCallExpression(
                        logVariable,
                        "is" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1, methodName.length()) + "Enabled",
                        ArgumentListExpression.EMPTY_ARGUMENTS);
            }

            return new TernaryExpression(
                    new BooleanExpression(condition),
                    originalExpression,
                    ConstantExpression.NULL);
        }
    }
}
