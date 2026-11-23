
package uz.demo.dockerlab.asynclab;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {

        // 1. Caller thread'dagi context
        Map<String, String> capturedContext =
                MDC.getCopyOfContextMap();

        return () -> {

            // 2. Worker'ning oldingi context'i
            Map<String, String> previousContext =
                    MDC.getCopyOfContextMap();

            try {

                // 3. Caller context'ini o'rnatamiz
                if (capturedContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(capturedContext);
                }

                // 4. Haqiqiy task
                task.run();

            } finally {

                // 5. Worker context'ini tiklaymiz
                if (previousContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousContext);
                }
            }
        };
    }
}
