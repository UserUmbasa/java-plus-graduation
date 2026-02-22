package ru.practicum.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.processor.EventSimilarityProcessor;
import ru.practicum.analyzer.processor.UserActionProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarter implements CommandLineRunner, DisposableBean {
    private final EventSimilarityProcessor eventSimilarityProcessor;
    private final UserActionProcessor userActionProcessor;

    private Thread eventSimilarityThread;
    private Thread userActionThread;

    @Override
    public void run(String... args) {
        eventSimilarityThread = new Thread(eventSimilarityProcessor, "event-similarity-processor");
        userActionThread = new Thread(userActionProcessor, "user-action-processor");

        log.info("Запуск EventSimilarityProcessor");
        eventSimilarityThread.start();

        log.info("Запуск UserActionProcessor");
        userActionThread.start();
    }

    @Override
    public void destroy() throws Exception {
        log.info("Остановка процессоров...");

        eventSimilarityProcessor.stop();
        userActionProcessor.stop();

        if (eventSimilarityThread != null) {
            eventSimilarityThread.join(10_000);
        }
        if (userActionThread != null) {
            userActionThread.join(10_000);
        }

        log.info("Процессоры остановлены");
    }
}