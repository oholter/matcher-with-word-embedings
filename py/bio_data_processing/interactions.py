from tqdm import tqdm
import pandas as pd
import numpy as np
from gensim.models import KeyedVectors
import logging
import math
from sklearn import svm


logging.basicConfig(format="%(asctime)s : %(levelname)s : %(message)s", level=logging.INFO)

model_file = "/home/ole/master/test_onto/model.bin"
protein_interactions_file = "/home/ole/master/bio_data/yeast.protein.links.v11.0.txt"

# 1) Read the embeddings from file
# 2) Remove unwanted information from interactions file
# 3) Read the protein protein interactions from file

def read_model(model_file):
    return KeyedVectors.load(model_file)


def remove_unwanted_information(interaction_file):
    lines = open(interaction_file, 'r').readlines()
    with tqdm(total=len(lines), desc="reading") as bar:
        for lnum in range(1,len(lines)):
            line = lines[lnum]
            line_content = line.split(" ")
            protein1 = line_content[0].split(".")[1]
            protein2 = line_content[1].split(".")[1]
            score = line_content[2]
            lines[lnum] = [protein1, protein2, score]
            bar.update(1)
    
    with open(interaction_file, 'w') as f:
        with tqdm(total=len(lines[1:]), desc="writing") as bar:
            f.write("protein1 protein2 combined_score\n")
            for line in lines[1:]:
                f.write(" ".join(line))
                bar.update(1)

def read_interactions_file(interactions_file):
    data = pd.read_csv(interactions_file, sep=" ")
    logging.info("TESTING: \n{}".format(data[['protein1', 'protein2']][data['protein1'] == 'YNL178W'].head()))
    return data

def get_positive_training_data_names(fraction, dataframe):
    sample = dataframe.sample(frac=fraction)
    p1 = sample['protein1'].values
    p2 = sample['protein2'].values
    
    for pair in zip(p1,p2):
        yield pair
        
def get_negative_training_data_names(fraction, dataframe, max_sample_size):
    sample = dataframe.sample(frac=fraction)
    p1 = sample['protein1'].values
    punique = np.unique(p1)
    #samples_per_unique = math.floor((dataframe.size * fraction) / np.shape(punique)[0])
    tot_samples = 0
    #print(samples_per_unique)
    for pname in punique:
        neg_samples = dataframe['protein2'][dataframe['protein1'] != pname].values
        samples_per_pname = 0
        for pair in zip([p1] * neg_samples.size, neg_samples):
            yield pair
            tot_samples += 1
            samples_per_pname += 1
            if samples_per_pname >= max_sample_size:
                break
        logging.info("generated negative samples: {}".format(tot_samples))
        if tot_samples >= fraction * dataframe.size:
            break
        

def get_training_data(fraction, model, dataframe):
    for i, row in enumerate(dataframe.itertuples()):
        if fraction * dataframe.size <= i:
            logging.info("total number of samples: {}".format(i-1))
            break;
        if i % 100 == 0:
            logging.info("created {} training samples".format(i))
        p1_name = row[1]
        p2_name = row[2]
        #if p1_name in model.wv.vocab and p2_name in model.wv.vocab:
            #p1_vector = model[p1_name]
            #p2_vector = model[p2_name]
            #yield [p1_vector, p2_vector], 1
        yield [p1_name, p2_name], 1
        p2_negative = dataframe['protein2'][dataframe['protein1'] != p1_name].sample(1)
        yield [p1_name, p2_negative.values[0]], 0
        #logging.info("random negative sample = {}: {}".format(p1_name, p2_negative.values[0]))
            
        #else:
        #    logging.info("Vectors for {0} and {1} NOT IN MODEL".format(p1_name, p2_name))
        #    continue

    
def train_model(examples):
    clf = svm.SVC(gamma='scale')
    x = examples[:,0]
    y = examples[:,1]
    clf.fit(x,y)
    return 0

def get_yeast_vocab(model):
    return [word for word in model.wv.vocab if 'yeast' in word]

def names_to_vectors(examples, namespace, model):
    names = examples[:,0]
    names = np.char.add(namespace, names)
    print(names)
    vec = np.array([name_to_vector(n, model) for n in names])
    print(vec)
    

def name_to_vector(name, model):
    if name in model.wv.vocab:
        return model[name]
    else:
        return []



if __name__ == "__main__":
    #remove_unwanted_information(protein_interactions_file)
    data = read_interactions_file(protein_interactions_file)
    model = read_model(model_file)
    #ex = np.array(list(get_negative_training_data_names(0.2, data, 100000)))
    pex = list(get_positive_training_data_names(0.2, data))
    #ex.extend(pex)
    all_ex = np.array(pex)
    
    all_ex = names_to_vectors(all_ex, "http://yeast#", model)
    
    train_model(all_ex)
    print("finished")
