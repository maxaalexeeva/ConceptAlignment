package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.floatCosineDistance
import com.github.jelmerk.knn.scalalike.SearchResult
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.OntologyAlignmentItem
import org.clulab.alignment.data.ontology.OntologyIdentifier

object OntologyIndex {
  val dimensions = 300

  type Index = HnswIndex[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.loadFromFile[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[OntologyAlignmentItem]): Index = {
    val index = HnswIndex[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index
  }

  def findNearest(index: Index, vector: Array[Float]): Seq[SearchResult[OntologyAlignmentItem, Float]] = {
    val maxHits = index.size

    findNearest(index, vector, maxHits, None)
  }

  def findNearest(index: Index, vector: Array[Float], maxHits: Int, thresholdOpt: Option[Float]): Seq[SearchResult[OntologyAlignmentItem, Float]] = {
    val nearest = index.findNearest(vector, k = maxHits)
    val largest = nearest.map { case SearchResult(item, value) =>
      SearchResult(item, 1f - value)
    }
    val best = thresholdOpt.map { threshold =>
      largest.filter { case SearchResult(_, value) =>
        // If there is a comparison, then only use real numbers.  NaN satisfies no threshold.
        !value.isNaN && value >= threshold
      }
    }
    .getOrElse(largest)

    best
  }
}
