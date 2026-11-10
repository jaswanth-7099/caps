package com.example.ocr.service;

public final class OCRMetrics {
    private OCRMetrics() {
    }

    public static ErrorRates calculate(String reference, String hypothesis) {
        int[] referenceChars = reference.codePoints().toArray();
        int[] hypothesisChars = hypothesis.codePoints().toArray();
        String normalizedReference = reference.strip();
        String normalizedHypothesis = hypothesis.strip();
        String[] referenceWords = normalizedReference.isEmpty()
                ? new String[0] : normalizedReference.split("\\s+");
        String[] hypothesisWords = normalizedHypothesis.isEmpty()
                ? new String[0] : normalizedHypothesis.split("\\s+");
        return new ErrorRates(
                ratio(editDistance(referenceChars, hypothesisChars), referenceChars.length, hypothesisChars.length),
                ratio(editDistance(referenceWords, hypothesisWords), referenceWords.length, hypothesisWords.length));
    }

    private static double ratio(int distance, int referenceLength, int hypothesisLength) {
        if (referenceLength == 0) {
            return hypothesisLength == 0 ? 0.0 : 1.0;
        }
        return (double) distance / referenceLength;
    }

    private static int editDistance(int[] reference, int[] hypothesis) {
        int[] previous = new int[hypothesis.length + 1];
        int[] current = new int[hypothesis.length + 1];
        for (int j = 0; j <= hypothesis.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= reference.length; i++) {
            current[0] = i;
            for (int j = 1; j <= hypothesis.length; j++) {
                int substitution = previous[j - 1] + (reference[i - 1] == hypothesis[j - 1] ? 0 : 1);
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[hypothesis.length];
    }

    private static int editDistance(String[] reference, String[] hypothesis) {
        int[] previous = new int[hypothesis.length + 1];
        int[] current = new int[hypothesis.length + 1];
        for (int j = 0; j <= hypothesis.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= reference.length; i++) {
            current[0] = i;
            for (int j = 1; j <= hypothesis.length; j++) {
                int substitution = previous[j - 1] + (reference[i - 1].equals(hypothesis[j - 1]) ? 0 : 1);
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[hypothesis.length];
    }
}
