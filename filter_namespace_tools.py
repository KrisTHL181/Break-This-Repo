from litellm.integrations.custom_logger import CustomLogger


class NamespaceToolFilter(CustomLogger):
    """Drop Codex namespace tools for upstreams that only accept function tools."""

    async def async_pre_call_hook(self, user_api_key_dict, cache, data, call_type):
        tools = data.get("tools")
        if not isinstance(tools, list):
            return data

        filtered = [
            tool
            for tool in tools
            if not (isinstance(tool, dict) and tool.get("type") == "namespace")
        ]

        if filtered:
            data["tools"] = filtered
        else:
            data.pop("tools", None)
            data.pop("tool_choice", None)

        return data


namespace_tool_filter = NamespaceToolFilter()
