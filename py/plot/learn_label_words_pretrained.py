import gensim
import logging
from gensim.models import Word2Vec
from gensim.models import KeyedVectors


logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

pretrained_model = "/home/ole/master/word2vec/models/fil9.model"
walks_document = '/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp/out/label.txt'

def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            #yield gensim.utils.simple_preprocess(line)
            yield line.split(' ')


logging.info("reading input_file: {}".format(walks_document))
documents = list(read_input(walks_document))


model = Word2Vec(size=100, min_count=1)
model.build_vocab(documents)
tot_examples = model.corpus_count

#pretrained_model = KeyedVectors.load_word2vec_format(pretrained_model, binary=True)
model.intersect_word2vec_format(pretrained_model, binary=True, lockf=1.0)
model.train(documents, total_examples=tot_examples, epochs=5)

#model.wv.save_word2vec_format('model.bin', binary=True)
model.save("label_pretrained.bin")