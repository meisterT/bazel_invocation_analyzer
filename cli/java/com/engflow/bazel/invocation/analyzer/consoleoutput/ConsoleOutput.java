/*
 * Copyright 2022 EngFlow Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.engflow.bazel.invocation.analyzer.consoleoutput;

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.suggestionproviders.SuggestionProviderUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsoleOutput {

  private static final String CONSOLE_FORMAT_DELIMITER = ";";
  private static final String CONSOLE_FORMAT_STRING = "\u001B[%sm%s\u001B[0m";
  private static final String DATA_HEADER = "Data from analysis";
  private static final String ERROR = "An error occurred while trying to analyze your profile.";
  private static final String HEADING_CAVEATS = "Caveats";
  private static final String HEADING_FAILURES = "Failures";
  private static final String HEADING_POTENTIAL_IMPROVEMENT = "Potential improvement";
  private static final String HEADING_RATIONALE = "Rationale";
  private static final String HEADING_SUGGESTION = "Suggestion";
  private static final String HEADING_SUGGESTION_OUTPUT_CAVEATS = "Analysis caveats";
  private static final String LIST_ITEM_PREFIX = "- ";
  private static final String NEWLINE = "\n";
  private static final String NO_SUGGESTIONS =
      "The tool did not produce any suggestions for this profile. Try again with a different"
          + " profile or use a later version of this tool.";
  private static final String NOTE = "Note";
  private static final String SUGGEST_VERBOSE_MODE =
      "Try running this tool with --verbose to get more data.";
  private static final String TITLE = "Bazel Invocation Analyzer by EngFlow";
  private static final String FEEDBACK_TITLE = "Help improve this tool";
  private static final String FEEDBACK_OPTIONS_INTRO =
      "You can provide feedback on Bazel Invocation Analyzer as follows:";
  private static final String FEEDBACK_OPTION_GITHUB_FORMAT = "%s%s to file an issue";
  private static final String FEEDBACK_OPTION_EMAIL_FORMAT = "%s%s to send us an email";
  private static final String FEEDBACK_CONTRIBUTE =
      "Bazel Invocation Analyzer is open source. We welcome contributions!";
  private static final String TAB = "\t";
  private static final String FEEDBACK_OPEN_NEW_ISSUE =
      "https://github.com/EngFlow/bazel_invocation_analyzer/issues";
  private static final String FEEDBACK_EMAIL_ADDRESS = "bazel-invocation-analyzer-dev@engflow.com";

  private final boolean disableFormatting;
  private final boolean verbose;

  public ConsoleOutput(boolean disableFormatting, boolean verbose) {
    this.disableFormatting = disableFormatting;
    this.verbose = verbose;
  }

  public void outputHeader() {
    System.out.println();
    System.out.println(
        format(TITLE, ConsoleOutputStyle.TEXT_BOLD, ConsoleOutputStyle.TEXT_UNDERLINE));
    System.out.println();
  }

  public void outputNote(String warning) {
    System.out.println(format(NOTE, ConsoleOutputStyle.TEXT_BOLD));
    System.out.println(warning);
    System.out.println();
  }

  public void outputFeedbackOptions() {
    System.out.println();
    System.out.println(formatAsHeading(FEEDBACK_TITLE));
    System.out.println(FEEDBACK_OPTIONS_INTRO);
    System.out.println(
        String.format(
            FEEDBACK_OPTION_GITHUB_FORMAT,
            TAB,
            format(FEEDBACK_OPEN_NEW_ISSUE, ConsoleOutputStyle.TEXT_UNDERLINE)));
    System.out.println(
        String.format(
            FEEDBACK_OPTION_EMAIL_FORMAT,
            TAB,
            format(FEEDBACK_EMAIL_ADDRESS, ConsoleOutputStyle.TEXT_UNDERLINE)));
    System.out.println(FEEDBACK_CONTRIBUTE);
    System.out.println();
  }

  public void outputAnalysisInput(String inputDescription) {
    System.out.println(
        format(String.format("Analyzing %s", inputDescription), ConsoleOutputStyle.TEXT_UNDERLINE));
  }

  public void outputSuggestions(List<SuggestionOutput> suggestionOutputs) {
    String topLevelCaveats =
        formatSuggestionOutputCaveats(
            suggestionOutputs.stream()
                .flatMap(suggestionOutput -> suggestionOutput.getCaveatList().stream())
                .collect(Collectors.toList()));
    if (!Strings.isNullOrEmpty(topLevelCaveats)) {
      System.out.println();
      System.out.print(topLevelCaveats);
    }
    ;

    String failures =
        formatFailures(
            suggestionOutputs.stream()
                .filter(suggestionOutput -> suggestionOutput.hasFailure())
                .map(suggestionOutput -> suggestionOutput.getFailure())
                .collect(Collectors.toList()));
    if (!Strings.isNullOrEmpty(failures)) {
      System.out.println();
      System.out.print(failures);
    }

    List<Suggestion> suggestions =
        suggestionOutputs.stream()
            .flatMap(suggestionOutput -> suggestionOutput.getSuggestionList().stream())
            .collect(Collectors.toList());
    String formattedSuggestions = formatSuggestions(suggestions);
    if (!Strings.isNullOrEmpty(formattedSuggestions)) {
      System.out.println(formattedSuggestions);
    } else {
      outputNote(NO_SUGGESTIONS);
    }
  }

  public void outputError(Throwable throwable) {
    System.err.println(format(ERROR, ConsoleOutputStyle.TEXT_RED));
    System.err.println(throwable.getMessage());
    if (verbose) {
      System.err.println(Throwables.getStackTraceAsString(throwable));
    }
  }

  public void outputAnalysisData(
      Map<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>> dataByProvider) {
    System.out.println();
    System.out.println(formatAsHeading(DATA_HEADER));
    System.out.println(formatAnalysisData(dataByProvider));
  }

  @VisibleForTesting
  String formatAnalysisData(
      Map<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>> dataByProvider) {
    if (verbose) {
      StringBuilder builder = new StringBuilder();
      dataByProvider.keySet().stream()
          .sorted(Comparator.comparing(p -> getClassName(p)))
          .forEach(
              provider -> {
                var data = dataByProvider.get(provider);
                var entries = formatData(data.entrySet().stream());
                if (!entries.isBlank()) {
                  builder.append(NEWLINE);
                  builder.append(format(getClassName(provider), ConsoleOutputStyle.BG_BLUE));
                  builder.append(NEWLINE);
                  builder.append(entries);
                }
              });
      return builder.toString();
    } else {
      return formatData(dataByProvider.values().stream().flatMap(x -> x.entrySet().stream()));
    }
  }

  private String getClassName(Class<?> clazz) {
    var name = clazz.getSimpleName();
    if (name.isEmpty()) {
      return "-anonymous-";
    }
    return name;
  }

  private String formatData(Stream<Map.Entry<Class<? extends Datum>, Datum>> data) {
    var builder = new StringBuilder();
    data.sorted(Comparator.comparing(es -> getClassName(es.getKey())))
        .forEach(es -> builder.append(formatDatum(es.getKey(), es.getValue())));
    return builder.toString();
  }

  private String formatDatum(Class<? extends Datum> clazz, Datum datum) {
    var description = datum.getDescription();
    var output = datum.isEmpty() && verbose ? datum.getEmptyReason() : datum.getSummary();
    if (!Strings.isNullOrEmpty(output)) {
      return String.format(
          "%s: %s%s%s%s",
          format(getClassName(clazz), ConsoleOutputStyle.TEXT_GREEN),
          format(description, ConsoleOutputStyle.TEXT_DIM),
          NEWLINE,
          output,
          NEWLINE);
    }
    return "";
  }

  @VisibleForTesting
  String formatSuggestions(List<Suggestion> suggestions) {
    StringBuilder sb = new StringBuilder();
    suggestions.stream()
        .sorted(
            (a, b) -> {
              double improvementA =
                  a.hasPotentialImprovement()
                      ? a.getPotentialImprovement().getDurationReductionPercentage()
                      : 0;
              double improvementB =
                  b.hasPotentialImprovement()
                      ? b.getPotentialImprovement().getDurationReductionPercentage()
                      : 0;
              return Double.compare(improvementB, improvementA);
            })
        .forEach(
            suggestion -> {
              if (Strings.isNullOrEmpty(suggestion.getRecommendation())) {
                return;
              }
              sb.append(NEWLINE);
              sb.append(NEWLINE);
              addSection(
                  sb,
                  String.format("%s: \"%s\"", HEADING_SUGGESTION, suggestion.getTitle()),
                  suggestion.getRecommendation(),
                  ConsoleOutputStyle.TEXT_GREEN);
              if (suggestion.hasPotentialImprovement()) {
                PotentialImprovement improvement = suggestion.getPotentialImprovement();
                List<String> improvementMessages = new ArrayList();
                double durationReductionPercentage = improvement.getDurationReductionPercentage();
                String durationMsg =
                    SuggestionProviderUtil.invocationDurationReductionMsg(
                        durationReductionPercentage);
                if (!Strings.isNullOrEmpty(durationMsg)) {
                  improvementMessages.add(durationMsg);
                  improvementMessages.add(
                      visualizeDurationReductionPercentage(durationReductionPercentage));
                }
                String message = improvement.getMessage();
                if (!Strings.isNullOrEmpty(message)) {
                  improvementMessages.add(message);
                }
                addSection(
                    sb, HEADING_POTENTIAL_IMPROVEMENT, String.join(NEWLINE, improvementMessages));
              }
              addSection(sb, HEADING_RATIONALE, suggestion.getRationaleList());
              List<String> formattedCaveats =
                  suggestion.getCaveatList().stream()
                      .map(caveat -> formatCaveat(caveat))
                      .collect(Collectors.toList());
              addSection(sb, HEADING_CAVEATS, formattedCaveats, ConsoleOutputStyle.TEXT_YELLOW);
            });
    return sb.toString();
  }

  @VisibleForTesting
  String formatSuggestionOutputCaveats(List<Caveat> caveats) {
    if (caveats.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    addSection(
        sb,
        HEADING_SUGGESTION_OUTPUT_CAVEATS,
        caveats.stream().map(caveat -> formatCaveat(caveat)).collect(Collectors.toList()),
        ConsoleOutputStyle.TEXT_YELLOW);
    return sb.toString();
  }

  @VisibleForTesting
  String formatFailures(List<SuggestionOutput.Failure> failures) {
    if (failures.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    addSection(
        sb,
        HEADING_FAILURES,
        failures.stream()
            .map(
                failure -> {
                  StringBuilder failureSb = new StringBuilder();
                  failureSb.append(failure.getMessage());
                  failureSb.append(NEWLINE);
                  if (verbose) {
                    failureSb.append(failure.getStackTrace());
                    failureSb.append(NEWLINE);
                  }
                  return failureSb.toString();
                })
            .collect(Collectors.toList()),
        ConsoleOutputStyle.TEXT_RED);
    return sb.toString();
  }

  private void addSection(
      StringBuilder sb, String heading, String entry, ConsoleOutputStyle... styles) {
    if (!Strings.isNullOrEmpty(entry)) {
      sb.append(formatAsHeading(heading))
          .append(NEWLINE)
          .append(format(entry, styles))
          .append(NEWLINE);
    }
  }

  private void addSection(
      StringBuilder sb, String heading, List<String> entries, ConsoleOutputStyle... styles) {
    addSection(sb, heading, String.join(NEWLINE, entries), styles);
  }

  private static String formatCaveat(Caveat caveat) {
    StringBuilder sb = new StringBuilder();
    boolean hasCustomMessage = !Strings.isNullOrEmpty(caveat.getMessage());
    if (hasCustomMessage) {
      sb.append(LIST_ITEM_PREFIX);
      sb.append(caveat.getMessage());
    }
    if (caveat.getSuggestVerboseMode()) {
      if (hasCustomMessage) {
        sb.append(" ");
      } else {
        sb.append(LIST_ITEM_PREFIX);
      }
      sb.append(SUGGEST_VERBOSE_MODE);
    }
    return sb.toString();
  }

  @VisibleForTesting
  // Prints out a simple visualization of the invocation's potential speed-up, where each symbol
  // represents 2% of the original duration.
  String visualizeDurationReductionPercentage(double percentage) {
    if (percentage < 0 || percentage > 100) {
      return null;
    }
    StringBuilder sb = new StringBuilder(52);
    sb.append("[");
    int eliminated = (int) Math.round(percentage) / 2;
    for (int i = 0; i < 50 - eliminated; i++) {
      sb.append(".");
    }
    for (int i = 50 - eliminated; i < 50; i++) {
      sb.append("X");
    }
    sb.append("]");
    return sb.toString();
  }

  @VisibleForTesting
  String formatAsHeading(String text) {
    return format(text, ConsoleOutputStyle.TEXT_BOLD);
  }

  @VisibleForTesting
  String format(String text, ConsoleOutputStyle... styles) {
    if (disableFormatting || styles.length == 0) {
      return text;
    }
    String styleString =
        String.join(
            CONSOLE_FORMAT_DELIMITER,
            Arrays.stream(styles)
                .map((style) -> String.valueOf(style.code))
                .collect(Collectors.toList()));
    return String.format(CONSOLE_FORMAT_STRING, styleString, text);
  }
}
