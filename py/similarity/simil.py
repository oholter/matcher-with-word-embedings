from gensim.models import KeyedVectors
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import click
import logging

logging.basicConfig(level=logging.INFO)

@click.command()
@click.argument("model_file", default="/home/ole/master/test_onto/model.bin")
@click.argument("ns1", default="cmt")
@click.argument("ns2", default="sigkdd")
def main(model_file, ns1, ns2):
    model = KeyedVectors.load_word2vec_format(model_file, binary=False)

    dim = model.vector_size
    first_vector = np.zeros(dim)
    second_vector = np.zeros(dim)
    
    num_first_concept = 0
    num_second_concept = 0

    for word in model.vocab:
        if ns1 in word:
            num_first_concept += 1
            first_vector += model[word]
            #print("adding word: {}".format(word))
        elif ns2 in word:
            #print("adding word: {}".format(word))
            num_second_concept += 1
            second_vector += model[word]
        else:
            logging.warn("Unknown namespace: {}".format(word))
    
    simil = np.dot(first_vector, second_vector) / (np.linalg.norm(first_vector) * np.linalg.norm(second_vector))
    print("Read {} concepts from first and {} concepts from second".format(num_first_concept, num_second_concept))
    print("Similarity: {}".format(simil))

if __name__ == "__main__":
    main()