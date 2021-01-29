package com.streamr.client.protocol.message_layer;

import java.util.List;
import java.util.Objects;

public final class GroupKeyErrorResponse extends AbstractGroupKeyMessage {
  private final String requestId;
  private final String code;
  private final String message;
  private final List<String> groupKeyIds;

  public GroupKeyErrorResponse(
      String requestId, String streamId, String code, String message, List<String> groupKeyIds) {
    super(streamId);
    Objects.requireNonNull(requestId, "requestId");
    this.requestId = requestId;
    Objects.requireNonNull(code, "code");
    this.code = code;
    Objects.requireNonNull(message, "message");
    this.message = message;
    ValidationUtil.checkNotEmpty(groupKeyIds, "groupKeyIds");
    this.groupKeyIds = groupKeyIds;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public List<String> getGroupKeyIds() {
    return groupKeyIds;
  }

  @Override
  protected StreamMessage.MessageType getMessageType() {
    return StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final GroupKeyErrorResponse that = (GroupKeyErrorResponse) o;
    return Objects.equals(requestId, that.requestId)
        && Objects.equals(code, that.code)
        && Objects.equals(message, that.message)
        && Objects.equals(groupKeyIds, that.groupKeyIds)
        && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, code, message, groupKeyIds, streamId);
  }

  @Override
  public String toString() {
    return String.format(
        "GroupKeyErrorResponse{requestId=%s, streamId=%s, code=%s, message=%s}",
        requestId, streamId, code, message);
  }
}
