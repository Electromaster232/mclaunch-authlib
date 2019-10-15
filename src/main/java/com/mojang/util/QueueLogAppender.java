package com.mojang.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "Queue", category = "Core", elementType = "appender", printObject = true)
public class QueueLogAppender extends AbstractAppender {
	
	private static final int MAX_CAPACITY = 250;
	private static final Map<String, BlockingQueue<String>> QUEUES;
	private static final ReadWriteLock QUEUE_LOCK;
	private final BlockingQueue<String> queue;
	
	static {
		QUEUES = new HashMap<String, BlockingQueue<String>>();
		QUEUE_LOCK = new ReentrantReadWriteLock();
	}
	
	public QueueLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, BlockingQueue<String> queue) {
		super(name, filter, layout, ignoreExceptions);
		this.queue = queue;
	}
	
	public void append(LogEvent logEvent) {
		if (this.queue.size() >= MAX_CAPACITY) {
			this.queue.clear();
		}
		this.queue.add(getLayout().toSerializable(logEvent).toString());
	}
	
	@PluginFactory
	public static QueueLogAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filters") Filter filter, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions, @PluginAttribute("target") String target) {
		if (name == null) {
			QueueLogAppender.LOGGER.error("No name provided for QueueLogAppender");
			return null;
		}
		
		if (target == null) {
			target = name;
		}
		
		QueueLogAppender.QUEUE_LOCK.writeLock().lock();
		BlockingQueue<String> queue = QueueLogAppender.QUEUES.get(target);
		if (queue == null) {
			queue = new LinkedBlockingQueue<String>();
			QueueLogAppender.QUEUES.put(target, queue);
		}
		
		QueueLogAppender.QUEUE_LOCK.writeLock().unlock();
		if (layout == null) {
			layout = PatternLayout.createLayout(null, null, null, null, null);
		}
		return new QueueLogAppender(name, filter, layout, ignoreExceptions, queue);
	}
	
	public static String getNextLogEvent(String queueName) {
		QueueLogAppender.QUEUE_LOCK.readLock().lock();
		BlockingQueue<String> queue = QueueLogAppender.QUEUES.get(queueName);
		QueueLogAppender.QUEUE_LOCK.readLock().unlock();
		
		if (queue != null) {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				QueueLogAppender.LOGGER.error("InterruptedException in QueueLogAppender");
			}
		}
		return null;
	}
}