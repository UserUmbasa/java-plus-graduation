package ru.practicum.requestservice.feignClients;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class UserActionClient {
    private final UserActionControllerGrpc.UserActionControllerBlockingStub stub;

    public UserActionClient(@GrpcClient("collector") UserActionControllerGrpc.UserActionControllerBlockingStub stub) {
        this.stub = stub;
    }

    public void sendView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW, "просмотр");
    }

    public void sendLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE, "лайк");
    }

    public void sendRegistration(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, "регистрацию");
    }

    // Универсальный приватный метод
    private void sendAction(Long userId, Long eventId, ActionTypeProto actionType, String logLabel) {
        Instant now = Instant.now(); //для точности
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        log.info("Передаем {} от пользователя: {}", logLabel, request);
        try {
            stub.collectUserAction(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке действия {} для пользователя {}: {}", actionType, userId, e.getMessage());
        }
    }
}

