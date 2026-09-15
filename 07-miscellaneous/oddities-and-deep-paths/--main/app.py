import streamlit as st
from openai import OpenAI

SYSTEM_PROMPT = """你是一款针对大一学生设计的文字冒险游戏主创（GM）。你需要围绕以下两个核心主题推进剧情：
1. 期末诚信考试教育（在剧情中体现作弊、抄袭等违纪行为的严重后果）；
2. 正确成长观（面对大一学业压力、挂科焦虑或未来选择时的脚踏实地）。
游戏规则：
- 每次给出一段客观平实的剧情描述，然后提供 A、B、C 三个选项。
- 玩家可能会直接点击 A/B/C 按钮，也可能会在文本框中输入完全不一样的自定义行动方案。
- 你需要根据玩家的选择（无论是选项还是自定义文本）灵活、客观地推进接下来的剧情。如果玩家的行为触发了严重违纪或急功近利，直接判定失败或游戏结束。
- 正常推进剧情时，可以提及违纪、失败等风险作为教育内容，但此时游戏仍在继续，必须继续给出 A、B、C 三个选项。
- 仅当本局真正结束（玩家失败、严重违纪被处分，或剧情圆满完结）时，在回复末尾单独一行输出「【游戏结束】」；其他情况下绝对不要输出该标记。
- 请直接给出游戏开场白和第一题。"""

GAME_OVER_MARKERS = ("【游戏结束】", "【本局结束】")


def inject_css() -> None:
    st.markdown(
        """
        <style>
            html, body, [class*="css"] {
                font-size: 1.4rem;
            }
            h1 {
                font-size: 2.4rem !important;
            }
            .history-container {
                min-height: 45vh;
                max-height: 55vh;
                overflow-y: auto;
                padding: 1.5rem;
                border: 2px solid #ddd;
                border-radius: 12px;
                background-color: #fafafa;
                margin-bottom: 1.5rem;
            }
            .history-msg {
                margin-bottom: 1.5rem;
                white-space: pre-wrap;
                line-height: 1.7;
            }
            .history-label {
                font-size: 1.2rem;
                font-weight: bold;
                margin-bottom: 0.4rem;
            }
            div[data-testid="column"] button[kind="secondary"],
            div[data-testid="column"] button[kind="primary"] {
                font-size: 1.4rem !important;
                padding: 1rem 2rem !important;
                min-height: 4rem !important;
            }
            div[data-testid="stForm"] button {
                font-size: 1.3rem !important;
                padding: 0.8rem 1.5rem !important;
                min-height: 3.5rem !important;
            }
            div[data-testid="stTextInput"] input {
                font-size: 1.3rem !important;
            }
            div[data-testid="stTextInput"] label {
                font-size: 1.3rem !important;
            }
        </style>
        """,
        unsafe_allow_html=True,
    )


def init_session_state() -> None:
    defaults = {
        "messages": [],
        "initialized": False,
        "game_over": False,
        "api_key": "",
        "base_url": "https://api.deepseek.com",
        "model": "deepseek-chat",
    }
    for key, value in defaults.items():
        if key not in st.session_state:
            st.session_state[key] = value


def call_llm() -> str | None:
    client = OpenAI(
        api_key=st.session_state.api_key,
        base_url=st.session_state.base_url.rstrip("/"),
    )
    api_messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    api_messages.extend(st.session_state.messages)
    response = client.chat.completions.create(
        model=st.session_state.model,
        messages=api_messages,
        temperature=0.8,
    )
    return response.choices[0].message.content.strip()


def check_game_over(text: str) -> None:
    if any(marker in text for marker in GAME_OVER_MARKERS):
        st.session_state.game_over = True


def handle_choice(choice: str) -> None:
    if st.session_state.game_over:
        return

    if choice in ("A", "B", "C"):
        user_msg = f"我选择选项 {choice}"
    else:
        user_msg = f"自定义行动：{choice}"

    st.session_state.messages.append({"role": "user", "content": user_msg})

    try:
        with st.spinner("剧情推进中..."):
            reply = call_llm()
    except Exception as e:
        st.session_state.messages.pop()
        st.error(f"接口请求失败：{e}")
        return

    if not reply:
        st.session_state.messages.pop()
        st.error("接口返回内容为空，请重试。")
        return

    st.session_state.messages.append({"role": "assistant", "content": reply})
    check_game_over(reply)
    st.rerun()


def render_history() -> None:
    parts = ['<div class="history-container">']
    if not st.session_state.messages:
        parts.append(
            '<p class="history-msg" style="color:#888;">'
            "暂无剧情记录，请在侧边栏填写接口密钥后开始游戏。"
            "</p>"
        )
    else:
        for msg in st.session_state.messages:
            if msg["role"] == "assistant":
                label = "🎮 GM"
            else:
                label = "👥 班级选择"
            content = (
                msg["content"]
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            )
            parts.append(
                f'<div class="history-msg">'
                f'<div class="history-label">{label}</div>'
                f"<div>{content}</div>"
                f"</div>"
            )
    parts.append("</div>")
    st.markdown("".join(parts), unsafe_allow_html=True)


def render_sidebar() -> bool:
    with st.sidebar:
        st.header("⚙️ 接口配置")
        st.session_state.api_key = st.text_input(
            "接口密钥",
            value=st.session_state.api_key,
            type="password",
            placeholder="请输入接口密钥",
        )
        st.session_state.base_url = st.text_input(
            "接口地址",
            value=st.session_state.base_url,
            placeholder="https://api.deepseek.com",
        )
        st.session_state.model = st.text_input(
            "模型名称",
            value=st.session_state.model,
            placeholder="例如：deepseek-chat",
        )

        st.divider()

        if st.button("重新开始游戏", use_container_width=True):
            st.session_state.messages = []
            st.session_state.initialized = False
            st.session_state.game_over = False
            st.rerun()

    return bool(st.session_state.api_key.strip())


def start_game_if_needed(api_ready: bool) -> None:
    if not api_ready or st.session_state.initialized:
        return

    try:
        with st.spinner("游戏加载中..."):
            reply = call_llm()
    except Exception as e:
        st.error(f"开局接口请求失败：{e}")
        return

    if reply:
        st.session_state.messages.append({"role": "assistant", "content": reply})
        st.session_state.initialized = True
        check_game_over(reply)


def main() -> None:
    st.set_page_config(
        page_title="大一班会 · 文字冒险",
        layout="wide",
        initial_sidebar_state="expanded",
    )
    inject_css()
    init_session_state()

    st.title("🎓 大一班会 · 文字冒险")

    api_ready = render_sidebar()
    start_game_if_needed(api_ready)

    if st.session_state.game_over:
        st.info("本局已结束，可在侧边栏点击「重新开始游戏」。")

    if not api_ready:
        st.warning("请先在侧边栏填写接口密钥以开始游戏。")

    st.subheader("📜 剧情记录")
    render_history()

    disabled = st.session_state.game_over or not api_ready

    st.subheader("🎯 做出选择")
    col_a, col_b, col_c = st.columns(3)
    if col_a.button("选项 A", use_container_width=True, disabled=disabled, key="btn_a"):
        handle_choice("A")
    if col_b.button("选项 B", use_container_width=True, disabled=disabled, key="btn_b"):
        handle_choice("B")
    if col_c.button("选项 C", use_container_width=True, disabled=disabled, key="btn_c"):
        handle_choice("C")

    with st.form("custom_choice_form", clear_on_submit=True):
        c1, c2 = st.columns([4, 1])
        custom = c1.text_input(
            "或者输入全班的自定义选择：",
            disabled=disabled,
            placeholder="例如：向老师坦白并申请补考",
        )
        submitted = c2.form_submit_button(
            "提交自定义选择",
            disabled=disabled,
            use_container_width=True,
        )

    if submitted:
        if custom.strip():
            handle_choice(custom.strip())
        else:
            st.warning("请输入自定义选择内容。")


if __name__ == "__main__":
    main()
