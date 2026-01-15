package com.uoa.core.nlg.utils

import com.uoa.core.model.TokenizerOutput
import java.util.*

class Tokenizer(private val vocab: Map<String, Int>) {

    fun encode(text: String, maxLength: Int = 7): TokenizerOutput {
        val tokens = tokenize(text)
        val tokenIds = tokens.map { vocab[it] ?: vocab["[UNK]"]!! } // [UNK] is used for unknown tokens

        // Pad or truncate token IDs to match the max length
        val paddedTokenIds = tokenIds.take(maxLength).toMutableList()
        while (paddedTokenIds.size < maxLength) {
            paddedTokenIds.add(vocab["[PAD]"]!!)
        }

        val attentionMask = paddedTokenIds.map { if (it == vocab["[PAD]"]) 0 else 1 }

        // Convert to LongArray
        val inputIdsLongArray = paddedTokenIds.map { it.toLong() }.toLongArray()
        val attentionMaskLongArray = attentionMask.map { it.toLong() }.toLongArray()

        return TokenizerOutput(
            inputIds = inputIdsLongArray,
            attentionMask = attentionMaskLongArray
        )
    }

    private fun tokenize(text: String): List<String> {
        val lowerCasedText = text.lowercase(Locale.ROOT)
        val words = lowerCasedText.split(Regex("\\s+"))

        // Split the text into words and subwords using the vocabulary
        return words.flatMap { word -> subWordTokenize(word) }
    }

    private fun subWordTokenize(word: String): List<String> {
        // If the word exists in the vocab, return it directly
        if (vocab.containsKey(word)) return listOf(word)

        // Otherwise, use a sub-word tokenization approach like WordPiece (simplified here)
        val subWords = mutableListOf<String>()
        var remainingWord = word

        while (remainingWord.isNotEmpty()) {
            var prefix = remainingWord
            while (prefix.isNotEmpty() && !vocab.containsKey(prefix)) {
                prefix = prefix.dropLast(1)
            }

            if (prefix.isEmpty()) {
                subWords.add("[UNK]")  // If no prefix matches, mark it as unknown
                break
            }

            subWords.add(prefix)
            remainingWord = remainingWord.removePrefix(prefix)
        }

        return subWords
    }
}
