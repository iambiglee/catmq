package com.baracklee.mq.biz;

import com.baracklee.mq.biz.common.util.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @Author： Barack Lee
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ClassLoaderUtilTest.class, ConsumerGroupUtilTest.class, HttpClientTest.class,
        UtilTest.class })
public class AllCoreTest {

}
