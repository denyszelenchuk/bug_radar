/** Copyright © 2015 Denys Zelenchuk.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.**/

package com.error.hunter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompleteReceiver extends BroadcastReceiver {   

    @Override  
    public void onReceive(Context context, Intent intent) {  
    	
    	if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {  
            Intent service = new Intent(context, ListenService.class);  
            context.startService(service);   
    	}
    }
}


