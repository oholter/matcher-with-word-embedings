from glove import Corpus, Glove
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

input_file = \
    '/home/ole/master/test_onto/walks_out.txt'

def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            #yield gensim.utils.simple_preprocess(line)
            yield [x.strip() for x in line.split(' ') if x.strip()]  

Corpus

glove = glove.Glove(lines, no_components=5, learning_rate=0.05)
glove.fit(corpus.matrix, epochs=30, no_threads=4, verbose=True)
glove.add_dictionary(corpus.dictionary)
glove.save("/home/ole/master/test_onto/model5.bin")

