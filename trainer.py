import logging
from gensim.models import Word2Vec
from gensim.utils import simple_preprocess

logging.basicConfig(format="%(asctime)s : %(levelname)s : %(message)s",
        level=logging.INFO)

FILE = "/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp"

def read_input(input_file):
    logging.info("Reading input from {}, please wait".format(input_file))
    with open(input_file, 'rb') as file:
        for i, line in enumerate(file):
            if i % 1000 == 0:
                logging.info("read {} reviews".format(i))
            yield simple_preprocess(line)

document = list(read_input(""))

