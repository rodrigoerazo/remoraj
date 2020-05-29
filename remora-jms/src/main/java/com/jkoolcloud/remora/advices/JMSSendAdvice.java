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

package com.jkoolcloud.remora.advices;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import javax.jms.*;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import com.jkoolcloud.remora.RemoraConfig;
import com.jkoolcloud.remora.core.EntryDefinition;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JMSSendAdvice extends BaseTransformers implements RemoraAdvice {

	public static final String ADVICE_NAME = "JMSSendAdvice";
	public static String[] INTERCEPTING_CLASS = { "javax.jms.MessageProducer" };
	public static String INTERCEPTING_METHOD = "send";

	@RemoraConfig.Configurable
	public static boolean logging = false;
	@RemoraConfig.Configurable
	private static boolean fetchMsg = false;
	public static TaggedLogger logger;
	static AgentBuilder.Transformer.ForAdvice advice = new AgentBuilder.Transformer.ForAdvice()
			.include(JMSSendAdvice.class.getClassLoader()) //
			.include(RemoraConfig.INSTANCE.classLoader) //
			.advice(methodMatcher(), JMSSendAdvice.class.getName());

	/**
	 * Method matcher intended to match intercepted class method/s to instrument. See (@ElementMatcher) for available
	 * method matches.
	 */

	private static ElementMatcher.Junction<NamedElement> methodMatcher() {
		return named(INTERCEPTING_METHOD);
	}

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
	public static void before(@Advice.This MessageProducer thiz, //
			@Advice.AllArguments Object[] arguments, //
			@Advice.Origin Method method, //
			@Advice.Local("ed") EntryDefinition ed, //
			@Advice.Local("startTime") long startTime) //
	// @Advice.Local("remoraLogger") Logger logger) //
	{
		try {
			if (!intercept(JMSSendAdvice.class, thiz, method, arguments)) {
				return;
			}
			if (logging) {
				logger.info("Entering: {} {} from {}", JMSSendAdvice.class.getSimpleName(), "before",
						thiz.getClass().getName());
			}

			ed = getEntryDefinition(ed, JMSSendAdvice.class, logging ? logger : null);
			ed.setEventType(EntryDefinition.EventType.SEND);
			startTime = fillDefaultValuesBefore(ed, stackThreadLocal, thiz, method, logging ? logger : null);

			if (thiz instanceof QueueSender) {
				String queueName = ((QueueSender) thiz).getQueue().getQueueName();
				ed.addPropertyIfExist("QUEUE", queueName);
				ed.addPropertyIfExist("QUEUE", queueName);
				ed.setResource(queueName, EntryDefinition.ResourceType.QUEUE);
			}

			for (Object argument : arguments) {
				if (argument instanceof Queue) {
					Queue destination = (Queue) argument;
					String queueName = destination.getQueueName();
					ed.addPropertyIfExist("QUEUE", queueName);
					ed.setResource(queueName, EntryDefinition.ResourceType.QUEUE);

				}
				if (argument instanceof Message) {
					Message message = (Message) argument;
					ed.addPropertyIfExist("MESSAGE_ID", message.getJMSMessageID());
					ed.addPropertyIfExist("CORR_ID", message.getJMSCorrelationID());
					ed.addPropertyIfExist("TYPE", message.getJMSType());
					if (fetchMsg && message instanceof TextMessage) {
						ed.addPropertyIfExist("MSG", ((TextMessage) message).getText());
					}
					try {
						message.setObjectProperty("JanusMessageSignature", ed.getCorrelator());
					} catch (Exception e) {
						logger.info("Cannot alter message");
					}
				}

			}

		} catch (Throwable t) {
			handleAdviceException(t, ADVICE_NAME, logging ? logger : null);
		}
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void after(@Advice.This MessageProducer obj, //
			@Advice.Origin Method method, //
			@Advice.AllArguments Object[] arguments, //
			@Advice.Thrown Throwable exception, //
			@Advice.Local("ed") EntryDefinition ed, //
			@Advice.Local("startTime") long startTime //
	// @Advice.Local("remoraLogger") Logger logger//
	) {
		boolean doFinally = true;
		try {
			if (!intercept(JMSSendAdvice.class, obj, method, arguments)) {
				return;
			}
			if (ed == null) // noinspection Duplicates
			{ // ed expected to be null if not created by entry, that's for duplicates
				if (logging) {
					logger.info("EntryDefinition not exist, entry might be filtered out as duplicate or ran on test");
				}
				doFinally = false;
				return;
			}
			// noinspection Duplicates
			if (logging) {
				logger.info("Exiting: {} {}", JMSSendAdvice.class.getName(), "after");
			}
			fillDefaultValuesAfter(ed, startTime, exception, logging ? logger : null);
		} catch (Throwable t) {
			handleAdviceException(t, ADVICE_NAME, logging ? logger : null);
		} finally {
			if (doFinally) {
				doFinally(logging ? logger : null, obj.getClass());
			}
		}

	}

	/**
	 * Type matcher should find the class intended for intrumentation See (@ElementMatcher) for available matches.
	 */

	@Override
	public ElementMatcher<TypeDescription> getTypeMatcher() {
		return hasSuperType(named(INTERCEPTING_CLASS[0]));
	}

	@Override
	public AgentBuilder.Transformer getAdvice() {
		return advice;
	}

	@Override
	protected AgentBuilder.Listener getListener() {
		return new TransformationLoggingListener(logger);
	}

	@Override
	public String getName() {
		return ADVICE_NAME;
	}

	@Override
	public void install(Instrumentation inst) {
		logger = Logger.tag(ADVICE_NAME);
		if (load) {
			getTransform().with(getListener()).installOn(inst);
		} else {
			logger.info("Advice {} not enabled", getName());
		}
	}
}
