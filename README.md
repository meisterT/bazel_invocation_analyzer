# Bazel Invocation Analyzer

The Bazel Invocation Analyzer is a library and terminal tool developed by EngFlow. It analyzes an invocation's Bazel profile and provides suggestions on how to speed up that invocation.
## Dependencies

[Bazel](https://bazel.build/) version 4.0+

## CLI

The Bazel Invocation Analyzer can be run in a terminal. In this mode it will print out the analysis results directly to the console.

### Usage

Pass in the path of a Bazel profile on your filesystem as the first argument. Use `-h` or `--help` to show all the available options.

```bash
bazel run //cli -- /path/to/bazel_profile.json.gz
```

## References

- [Generating a Bazel profile](https://bazel.build/rules/performance#performance-profiling)
- [Interpreting a Bazel profile](https://bazel.build/rules/performance#profile-information)
