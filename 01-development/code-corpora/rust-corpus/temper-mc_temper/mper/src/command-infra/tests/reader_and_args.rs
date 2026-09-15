use temper_command_infra::args::{
    GreedyStringArg, IntegerArg, PositionArg, QuotableStringArg, SingleWordArg,
};
use temper_command_infra::{CommandArg, CommandReader};

#[test]
fn reader_supports_checkpoint_and_rewind() {
    let mut reader = CommandReader::new("alpha beta");
    let checkpoint = reader.checkpoint();

    assert_eq!(reader.read_word_span().unwrap(), "alpha");
    assert_eq!(reader.cursor(), 5);

    reader.rewind(checkpoint);

    assert_eq!(reader.cursor(), 0);
    assert_eq!(reader.read_word_span().unwrap(), "alpha");
}

#[test]
fn reader_reads_quoted_strings() {
    let mut reader = CommandReader::new("\"hello \\\"there\\\"\" tail");
    let span = reader.read_quoted_string_span().unwrap();

    assert_eq!(span, "hello \\\"there\\\"");
    assert_eq!(reader.read_word_span().unwrap(), "tail");
}

#[test]
fn reader_rejects_unterminated_quoted_strings() {
    let mut reader = CommandReader::new("\"hello");
    let err = reader.read_quoted_string_span().unwrap_err();

    assert_eq!(err.expected, "closing quote");
}

#[test]
fn word_arg_consumes_one_token() {
    let mut reader = CommandReader::new("hello there");
    let raw = SingleWordArg::recognize(&mut reader).unwrap();
    let parsed = SingleWordArg::parse(raw).unwrap();

    assert_eq!(&*parsed, "hello");
    assert_eq!(reader.read_word_span().unwrap(), "there");
}

#[test]
fn quoted_string_arg_can_appear_before_later_args() {
    let mut reader = CommandReader::new("\"hello there\" 5");
    let raw_string = QuotableStringArg::recognize(&mut reader).unwrap();
    let raw_int = IntegerArg::<0, 10>::recognize(&mut reader).unwrap();

    let string = QuotableStringArg::parse(raw_string).unwrap();
    let int = IntegerArg::<0, 10>::parse(raw_int).unwrap();

    assert_eq!(&*string, "hello there");
    assert_eq!(*int, 5);
}

#[test]
fn greedy_arg_consumes_remainder() {
    let mut reader = CommandReader::new("hello there friend");
    let raw = GreedyStringArg::recognize(&mut reader).unwrap();
    let parsed = GreedyStringArg::parse(raw).unwrap();

    assert_eq!(&*parsed, "hello there friend");
    assert!(reader.expect_end().is_ok());
}

#[test]
fn position_arg_consumes_exactly_three_tokens() {
    let mut reader = CommandReader::new("~ ~1 3 Steve");
    let raw = PositionArg::recognize(&mut reader).unwrap();
    let position = PositionArg::parse(raw).unwrap();

    assert_eq!(position.x, "~");
    assert_eq!(position.y, "~1");
    assert_eq!(position.z, "3");
    assert_eq!(reader.read_word_span().unwrap(), "Steve");
}
