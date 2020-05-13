#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright 2019-2020 NASTEL TECHNOLOGIES, INC.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package ${package}.advices;

import net.bytebuddy.asm.Advice;
import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import com.jkoolcloud.remora.RemoraConfig;
import com.jkoolcloud.remora.core.EntryDefinition;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.description.method.MethodDescription;


public class ${adviceClassName}Advice extends BaseTransformers implements RemoraAdvice {

	public static final String ADVICE_NAME = "${adviceClassName}Advice";
	public static String[] INTERCEPTING_CLASS = { "<CHANGE HERE>" };
	public static String INTERCEPTING_METHOD = "<CHANGE HERE>";

	public static boolean logging = false;
	public static TaggedLogger logger;

	/**
	 * Method matcher intended to match intercepted class method/s to
	 * instrument. See (@ElementMatcher) for available method matches.
	 */

	private static ElementMatcher<? super MethodDescription> methodMatcher() {
		return named(INTERCEPTING_METHOD);
	}

	/**
	 * Type matcher should find the class intended for instrumentation See (@ElementMatcher) for available matches.
	 */

	@Override
	public ElementMatcher<TypeDescription> getTypeMatcher() {
		return hasSuperType(named(INTERCEPTING_CLASS[0]));
	}

	@Override
	public AgentBuilder.Transformer getAdvice() {
		return advice;
	}

	static AgentBuilder.Transformer.ForAdvice advice = new AgentBuilder.Transformer.ForAdvice()
		.include(${adviceClassName}Advice.class.getClassLoader())
        .include(RemoraConfig.INSTANCE.classLoader)//
		.advice(methodMatcher(), ${adviceClassName}Advice.class.getName());


	/**
	 * Advices before method is called before instrumented method code
	 *
	 * @param thiz
	 *            reference to method object
	 * @param arguments
	 *            arguments provided for method
	 * @param method
	 *            instrumented method description
	 * @param ed
	 *            {@link EntryDefinition} for collecting ant passing values to
	 *            {@link com.jkoolcloud.remora.core.output.OutputManager}
	 * @param startTime
	 *            method startTime
	 *
	 */

	@Advice.OnMethodEnter
	public static void before(@Advice.This Object thiz, //
			@Advice.AllArguments Object[] arguments, //
			@Advice.Origin Method method, //
			@Advice.Local("ed") EntryDefinition ed, //
			@Advice.Local("startTime") long startTime) {
		try {
            if (!getAdviceInstance(${adviceClassName}Advice.class).enabled) {
                return;
            }
			ed = getEntryDefinition(ed, ${adviceClassName}Advice.class, logging ? logger : null);;
			if (logging) {
				logger.info("Entering: {} {}",${adviceClassName}Advice.class.getName(), "before"));
			}
			startTime = fillDefaultValuesBefore(ed, stackThreadLocal, thiz, method, logging ? logger : null);
		} catch (Throwable t) {
			handleAdviceException(t, ADVICE_NAME, logging ? logger : null  );
		}
	}

	/**
	 * Method called on instrumented method finished.
	 *
	 * @param obj
	 *            reference to method object
	 * @param method
	 *            instrumented method description
	 * @param arguments
	 *            arguments provided for method
	 * @param exception
	 *            exception thrown in method exit (not caught)
	 * @param ed    {@link EntryDefinition} passed along the method (from before method)
	 * @param startTime startTime passed along the method
	 */

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void after(@Advice.This Object obj, //
			@Advice.Origin Method method, //
			@Advice.AllArguments Object[] arguments, //
			// @Advice.Return Object returnValue, // //TODO needs separate Advice capture for void type
			@Advice.Thrown Throwable exception, @Advice.Local("ed") EntryDefinition ed, //
			@Advice.Local("startTime") long startTime) {
		boolean doFinally = true;
		try {
		    if (!getAdviceInstance(${adviceClassName}Advice.class).enabled) {
             return;
            }
			if (ed == null) { // ed expected to be null if not created by entry, that's for duplicates
				if (logging) {
					logger.info("EntryDefinition not exist, entry might be filtered out as duplicate or ran on test");
				}
				doFinally = false;
				return;
			}
			if (logging) {
				logger.info("Exiting: {} {}",${adviceClassName}Advice.class.getName(), "after"));
			}
			fillDefaultValuesAfter(ed, startTime, exception, logging ? logger : null );
		} catch (Throwable t) {
			handleAdviceException(t, ADVICE_NAME, logging ? logger : null  );
		} finally {
			if (doFinally) {
				doFinally(logging ? logger : null, obj.getClass());
			}
		}

	}

	@Override
	protected AgentBuilder.Listener getListener() {
		return new TransformationLoggingListener(logger);
	}

	@Override
	public void install(Instrumentation instrumentation) {
		logger = Logger.tag(ADVICE_NAME);
		if (load) {
			getTransform().with(getListener()).installOn(instrumentation);
		} else {
		    logger.info("Advice {} not enabled", getName());
		}
	}

	@Override
	public String getName() {
		return ADVICE_NAME;
	}

}
