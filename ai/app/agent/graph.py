import operator
from typing import Annotated, List, TypedDict, Union
from langchain_openai import ChatOpenAI
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode
from app.tools.hr_tools import query_order_status, list_recent_orders, check_employee_eligibility, search_hr_knowledge

class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], operator.add]
    intent: str
    context: dict

# Define the tools
tools = [query_order_status, list_recent_orders, check_employee_eligibility, search_hr_knowledge]
tool_node = ToolNode(tools)

def get_model():
    # Use environment variable for model name, default to gpt-4o or similar
    return ChatOpenAI(model="gpt-4o", temperature=0).bind_tools(tools)

async def call_model(state: AgentState):
    messages = state['messages']
    model = get_model()
    response = await model.ainvoke(messages)
    return {"messages": [response]}

def should_continue(state: AgentState):
    messages = state['messages']
    last_message = messages[-1]
    if last_message.tool_calls:
        return "tools"
    return END

# Build the graph
workflow = StateGraph(AgentState)

workflow.add_node("agent", call_model)
workflow.add_node("tools", tool_node)

workflow.set_entry_point("agent")
workflow.add_conditional_edges(
    "agent",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)
workflow.add_edge("tools", "agent")

graph = workflow.compile()
