package com.baracklee.mq.biz.dto.client;


import com.baracklee.mq.biz.dto.BaseResponse;

public class GetMessageCountResponse extends BaseResponse {
	
	private long count;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

}
