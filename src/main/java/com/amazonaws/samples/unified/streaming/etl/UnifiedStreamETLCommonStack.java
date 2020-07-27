package com.amazonaws.samples.unified.streaming.etl;

import software.amazon.awscdk.core.CfnParameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.kinesis.Stream;




public class UnifiedStreamETLCommonStack extends Stack {
	
	public String unifiedOrderStreamArn="";
	public Object unifiedOrderStreamShardCount="";
	public String unifiedOrderStreamName="";
	
	

	public UnifiedStreamETLCommonStack(final Construct parent, final String id) {
		this(parent, id, null);
	}

	public UnifiedStreamETLCommonStack(final Construct parent, final String id, final StackProps props) {
		super(parent, id, props);
		
		
		
		CfnParameter orderStreamName = CfnParameter.Builder.create(this, "unifiedOrderStreamName")
		        .type("String")
		        .description("The name of the kinesis order stream").defaultValue("unifiedOrderStream")
		        .build();
		
		CfnParameter orderStreamShards = CfnParameter.Builder.create(this, "unifiedOrderStreamShards")
		        .type("Number")
		        .description("Number of shards for kinesis order stream").defaultValue(2)
		        .build();
		
	
		
		
		//create kinesis streams

		Stream orderStream = Stream.Builder.create(this, "unifiedOrderStreamId").streamName(orderStreamName.getValueAsString()).shardCount(orderStreamShards.getValueAsNumber()).build();
		unifiedOrderStreamArn = orderStream.getStreamArn();
		
		unifiedOrderStreamShardCount = orderStreamShards.getValueAsNumber();
		unifiedOrderStreamName = orderStreamName.getValueAsString();
		
		
		
		
	        
	       
		 
		
	}
	
	
}
