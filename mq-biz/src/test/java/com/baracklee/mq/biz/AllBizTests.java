package com.baracklee.mq.biz;

import com.baracklee.mq.biz.service.impl.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @Author： Barack Lee
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ AuditLogServiceImplTest.class, ConsumerCommitServiceImplTest.class,
        ConsumerGroupCheckServiceImplTest.class, ConsumerGroupConsumerCheckServiceImplTest.class,
        ConsumerGroupServiceImplTest.class, ConsumerGroupTopicCheckServiceImplTest.class,
        ConsumerGroupTopicServiceImplTest.class, ConsumerGroupTopicServiceImplTest.class, ConsumerServiceImplTest.class,
        DbNodeServiceImplTest.class, DbNodeServiceImplTest.class, EmailServiceImplTest.class, LogServiceImplTest.class,
        Message01ServiceImplTest.class })
public class AllBizTests {
}
