use crate::{ArgumentSpec, CommandPath, CommandPathSegment, EntityProperties, ParserProperties};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CommandNodeKind {
    Root,
    Literal,
    Argument,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CommandNode {
    pub kind: CommandNodeKind,
    pub name: Option<String>,
    pub argument: Option<ArgumentSpec>,
    pub children: Vec<usize>,
    pub executable: bool,
}

impl CommandNode {
    fn root() -> Self {
        Self {
            kind: CommandNodeKind::Root,
            name: None,
            argument: None,
            children: Vec::new(),
            executable: false,
        }
    }

    fn literal(name: &str) -> Self {
        Self {
            kind: CommandNodeKind::Literal,
            name: Some(name.to_string()),
            argument: None,
            children: Vec::new(),
            executable: false,
        }
    }

    fn argument(name: &str, spec: ArgumentSpec) -> Self {
        Self {
            kind: CommandNodeKind::Argument,
            name: Some(name.to_string()),
            argument: Some(spec),
            children: Vec::new(),
            executable: false,
        }
    }

    fn matches_segment(&self, segment: &CommandPathSegment) -> bool {
        match segment {
            CommandPathSegment::Literal { name, .. } => {
                self.kind == CommandNodeKind::Literal && self.name.as_deref() == Some(*name)
            }
            CommandPathSegment::Argument { spec, .. } => {
                self.kind == CommandNodeKind::Argument && self.argument == Some(*spec)
            }
        }
    }

    fn child_priority(&self) -> u8 {
        match self.kind {
            CommandNodeKind::Literal => 0,
            CommandNodeKind::Argument
                if matches!(
                    self.argument.and_then(|arg| arg.properties),
                    Some(ParserProperties::Entity(EntityProperties {
                        single: false,
                        ..
                    }))
                ) =>
            {
                1
            }
            CommandNodeKind::Argument
                if self
                    .argument
                    .and_then(|arg| arg.protocol_suggestions)
                    .is_some() =>
            {
                2
            }
            CommandNodeKind::Argument => 3,
            CommandNodeKind::Root => 4,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CommandGraph {
    pub nodes: Vec<CommandNode>,
    pub root_idx: usize,
}

impl Default for CommandGraph {
    fn default() -> Self {
        Self {
            nodes: vec![CommandNode::root()],
            root_idx: 0,
        }
    }
}

impl CommandGraph {
    pub fn from_paths(paths: &[CommandPath]) -> Self {
        let mut graph = Self::default();
        for path in paths {
            graph.push_path(path);
        }
        graph
    }

    pub fn push_path(&mut self, path: &CommandPath) {
        let mut current = self.root_idx;

        for literal in path.root.split_whitespace() {
            current = self.push_or_reuse(current, CommandPathSegment::literal(literal));
        }

        for segment in &path.segments {
            current = self.push_or_reuse(current, segment.clone());
        }

        self.nodes[current].executable = true;
    }

    fn push_or_reuse(&mut self, parent: usize, segment: CommandPathSegment) -> usize {
        for child_idx in self.nodes[parent].children.clone() {
            if self.nodes[child_idx].matches_segment(&segment) {
                return child_idx;
            }
        }

        let node = match segment {
            CommandPathSegment::Literal { name, .. } => CommandNode::literal(name),
            CommandPathSegment::Argument { name, spec, .. } => CommandNode::argument(name, spec),
        };

        let priority = node.child_priority();
        let insert_at = self.nodes[parent]
            .children
            .iter()
            .position(|child_idx| self.nodes[*child_idx].child_priority() > priority)
            .unwrap_or(self.nodes[parent].children.len());

        let idx = self.nodes.len();
        self.nodes.push(node);
        self.nodes[parent].children.insert(insert_at, idx);
        idx
    }
}
