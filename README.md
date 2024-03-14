<div align="center">
<h1 align="center">CatMQ</h1>

[English](./README.md) / [简体中文](./README_CN.md)

Lightweight, distributed message queue with visual operation and maintenance
</div>

## Features

- **High Performance**: CatMQ uses the latest technologies, enabling it to rapidly process a large number of messages.
- **Ease of Use**: CatMQ's API is intuitively designed, allowing developers to get started quickly.
- **Distributed**: CatMQ supports distributed systems, allowing for easy scaling across multiple servers.
- **Comprehensive Web Management Console**: CatMQ features a comprehensive web management console, allowing users to manipulate producers, consumers, queues, offsets, and other settings directly from the interface.
- **Sequential Messages**: CatMQ supports FIFO (First In, First Out) model for sequential messages, ensuring that messages are consumed in order within a ConsumerGroup.
- **Delayed Messages**: To satisfy scenarios requiring delayed consumption, CatMQ allows setting a delay time for message consumption.
- **Load Balancing of Consumers and Queues**: CatMQ enables dynamic adjustments to the number of consumers and message queues to cope with excessive consumption backlog or accidental downtime of consumers.
- **Multi-Consumer Group Subscriptions**: A queue can be simultaneously subscribed to by multiple consumer groups, with each group's consumption not affecting others.
- **Consumption Progress Adjustment**: CatMQ supports real-time dynamic adjustment of the consumption progress of a consumer group. The adjusted consumption progress takes effect immediately.
- **Message Storage and Scheduled Cleanup**: CatMQ uses a database to persist messages, setting an expiration time for each message. Expired messages are automatically removed to prevent data overflow.


## Roadmap
- [x] Completed the basic Kafka-like design and development.
- [x] Adapted screen development.
- [x] Added a timed consumption thread to clean up long-unused information.
- [x] Added an automatic system configuration check thread.
- [x] Wrote documentation, published the website, making it easier for more people to understand the project.
- [ ] Kakfa has new incremental rebalance protocol[KIP-429](https://cwiki.apache.org/confluence/display/KAFKA/KIP-429%3A+Kafka+Consumer+Incremental+Rebalance+Protocol), match it
- [ ] Adapting to JDK21, hoping to support both JDK8 and JDK21 simultaneously.

## Install
[快速启动](https://iambiglee.github.io/docs/example/quickstart/)  
[QuickStart for English user](https://iambiglee.github.io/en/docs/example/quickstart/)

## Document
You can see the all information in [CatMQ](https://iambiglee.github.io/en/docs/example/introduce/) ，and Chinese version is [中文文档](https://iambiglee.github.io/docs/example/introduce/)

## Contribute
The CatMQ project welcomes everyone to participate in maintenance. You can participate by submitting an Issue or RP.

## License
Apache License, Version 2.0 Copyright (C) Apache Software Foundation.[Apache License](https://www.apache.org/licenses/LICENSE-2.0.html)
