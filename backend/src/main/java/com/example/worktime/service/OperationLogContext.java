package com.example.worktime.service;

import com.example.worktime.model.OperationLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-local context object used to enrich automatically captured operation logs.
 */
@Component
public class OperationLogContext {
    private static final ThreadLocal<ContextState> STATE = ThreadLocal.withInitial(ContextState::new);

    public void reset() {
        STATE.remove();
    }

    private ContextState state() {
        return STATE.get();
    }

    public void setModule(String module) {
        state().module = normalize(module);
    }

    public void setEntity(String type, String id) {
        state().entityType = normalize(type);
        state().entityId = normalize(id);
    }

    public void setSummary(String summary) {
        state().summary = normalize(summary);
    }

    public void setTraceId(String traceId) {
        state().traceId = normalize(traceId);
    }

    public String getTraceId() {
        ContextState s = state();
        if (!StringUtils.hasText(s.traceId)) {
            return null;
        }
        return s.traceId;
    }

    public void appendDetail(String detail) {
        if (!StringUtils.hasText(detail)) {
            return;
        }
        state().details.add(detail.trim());
    }

    public void apply(OperationLog log) {
        ContextState snapshot = state().copy();
        if (StringUtils.hasText(snapshot.module)) {
            log.setModule(snapshot.module);
        }
        if (StringUtils.hasText(snapshot.summary) && !StringUtils.hasText(log.getSummary())) {
            log.setSummary(snapshot.summary);
        }
        if (StringUtils.hasText(snapshot.entityType)) {
            log.setEntityType(snapshot.entityType);
        }
        if (StringUtils.hasText(snapshot.entityId)) {
            log.setEntityId(snapshot.entityId);
        }
        if (StringUtils.hasText(snapshot.traceId) && !StringUtils.hasText(log.getTraceId())) {
            log.setTraceId(snapshot.traceId);
        }
        if (!snapshot.details.isEmpty()) {
            String existing = log.getDetails();
            String appended = String.join("\n", snapshot.details);
            if (StringUtils.hasText(existing)) {
                log.setDetails(existing + "\n" + appended);
            } else {
                log.setDetails(appended);
            }
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static class ContextState {
        private String module;
        private String entityType;
        private String entityId;
        private String summary;
        private String traceId;
        private List<String> details = new ArrayList<>();

        ContextState copy() {
            ContextState copy = new ContextState();
            copy.module = this.module;
            copy.entityType = this.entityType;
            copy.entityId = this.entityId;
            copy.summary = this.summary;
            copy.traceId = this.traceId;
            copy.details = new ArrayList<>(this.details);
            return copy;
        }
    }
}

