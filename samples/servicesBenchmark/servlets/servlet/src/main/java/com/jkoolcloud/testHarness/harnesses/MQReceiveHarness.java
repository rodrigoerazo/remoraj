/*
 *
 * Copyright (c) 2019-2020 NasTel Technologies, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of NasTel
 * Technologies, Inc. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with NasTel
 * Technologies.
 *
 * NASTEL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. NASTEL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 */

package com.jkoolcloud.testHarness.harnesses;

import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

import com.ibm.mq.jms.MQQueue;

public class MQReceiveHarness extends BaseMQHarness {

	@Configurable
	public long receiveTimeout = 100L;

	private MessageConsumer consumer;

	@Override
	String call_() throws Exception {
		TextMessage message = (TextMessage) consumer.receive(receiveTimeout);
		if (message != null) {
			message.acknowledge();
		}
		return message == null ? "No message" : message.getText();
	}

	@Override
	public void setup() throws Exception {
		super.setup();
		MQQueue queue = new MQQueue(queueManager, destination);
		consumer = session.createConsumer(queue);
	}

}