package com.jkoolcloud.remora.core;

import java.util.Stack;

import org.tinylog.TaggedLogger;

public class CallStack<T> extends Stack<EntryDefinition> {
	private final TaggedLogger logger;

	private String application;

	private String server;

	private String stackCorrelator;

	public CallStack(TaggedLogger logger) {
		this.logger = logger;
		stackCorrelator = new JUGFactoryImpl().newUUID();
	}

	@Override
	public EntryDefinition push(EntryDefinition item) {
		if (logger != null) {
			logger.info("Stack push: " + (size() + 1));
		}
		item.setApplication(application);
		item.setServer(server);
		item.setCorrelator(stackCorrelator);

		return super.push(item);
	}

	@Override
	public synchronized EntryDefinition pop() {
		if (logger != null) {
			logger.info("Stack pop: " + (size() - 1));
		}
		return super.pop();
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
		for (int i = 0; i < size(); i++) {
			get(i).setApplication(application);
		}
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
		for (int i = 0; i < size(); i++) {
			get(i).setServer(server);
		}
	}
}
