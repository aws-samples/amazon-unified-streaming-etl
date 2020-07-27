
package com.amazonaws.samples.unified.streaming.etl;


import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.core.App;

public class UnifiedStreamETLApp {
	
	public static void main(final String[] args) {
        App app = new App();
        

        UnifiedStreamETLCommonStack cstack = new UnifiedStreamETLCommonStack(app, "UnifiedStreamETLCommonStack");
        
        Map<String, Object> cmnStkResMap = new HashMap<String, Object>();
        cmnStkResMap.put("unifiedOrderStreamArn", cstack.unifiedOrderStreamArn);
        cmnStkResMap.put("unifiedOrderStreamName", cstack.unifiedOrderStreamName);
        cmnStkResMap.put("unifiedOrderStreamShardCount", cstack.unifiedOrderStreamShardCount);
        
        UnifiedStreamETLProcessStack pstack = new UnifiedStreamETLProcessStack(app, "UnifiedStreamETLProcessStack", cmnStkResMap);
        
        UnifiedStreamETLDataStack dstack = new UnifiedStreamETLDataStack(app, "UnifiedStreamETLDataStack", cmnStkResMap);
       
        pstack.addDependency(cstack);
        dstack.addDependency(cstack);

        app.synth();
    }

}
