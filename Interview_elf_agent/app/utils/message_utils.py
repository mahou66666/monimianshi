from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, BaseMessage


def sanitize_messages(messages: list[BaseMessage]) -> list[BaseMessage]:
    """Strip unsupported fields (e.g., name) from messages for strict providers."""
    cleaned: list[BaseMessage] = []
    for msg in messages:
        if isinstance(msg, HumanMessage):
            cleaned.append(HumanMessage(content=msg.content))
        elif isinstance(msg, AIMessage):
            cleaned.append(AIMessage(content=msg.content))
        elif isinstance(msg, SystemMessage):
            cleaned.append(SystemMessage(content=msg.content))
        else:
            cleaned.append(msg)
    return cleaned
