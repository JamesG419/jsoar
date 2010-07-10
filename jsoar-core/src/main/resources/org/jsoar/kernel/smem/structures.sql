# semantic_memory.cpp:smem_statement_container::smem_statement_container
# These are all the add_structure statements for initializing the smem 
# database
CREATE TABLE smem2_vars (id INTEGER PRIMARY KEY,value NONE)
CREATE TABLE smem2_temporal_symbol_hash (id INTEGER PRIMARY KEY, sym_const NONE, sym_type INTEGER)
CREATE UNIQUE INDEX smem2_temporal_symbol_hash_const_type ON smem2_temporal_symbol_hash (sym_type,sym_const)

CREATE TABLE smem2_lti (id INTEGER PRIMARY KEY, letter INTEGER, num INTEGER, child_ct INTEGER, act_cycle INTEGER)
CREATE UNIQUE INDEX smem2_lti_letter_num ON smem2_lti (letter, num)

CREATE TABLE smem2_web (parent_id INTEGER, attr INTEGER, val_const INTEGER, val_lti INTEGER, act_cycle INTEGER)
CREATE INDEX smem2_web_parent_attr_val_lti ON smem2_web (parent_id, attr, val_const, val_lti)
CREATE INDEX smem2_web_attr_val_lti_cycle ON smem2_web (attr, val_const, val_lti, act_cycle)
CREATE INDEX smem2_web_attr_cycle ON smem2_web (attr, act_cycle)

CREATE TABLE smem2_ct_attr (attr INTEGER PRIMARY KEY, ct INTEGER)

CREATE TABLE smem2_ct_const (attr INTEGER, val_const INTEGER, ct INTEGER)
CREATE UNIQUE INDEX smem2_ct_const_attr_val ON smem2_ct_const (attr, val_const)

CREATE TABLE smem2_ct_lti (attr INTEGER, val_lti INTEGER, ct INTEGER)
CREATE UNIQUE INDEX smem2_ct_lti_attr_val ON smem2_ct_lti (attr, val_lti) 

# adding an ascii table just to make lti queries easier when inspecting database
CREATE TABLE smem2_ascii (ascii_num INTEGER PRIMARY KEY, ascii_chr TEXT)
DELETE FROM smem2_ascii

INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (65,'A')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (66,'B')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (67,'C')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (68,'D')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (69,'E')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (70,'F')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (71,'G')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (72,'H')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (73,'I')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (74,'J')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (75,'K')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (76,'L')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (77,'M')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (78,'N')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (79,'O')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (80,'P')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (81,'Q')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (82,'R')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (83,'S')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (84,'T')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (85,'U')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (86,'V')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (87,'W')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (88,'X')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (89,'Y')
INSERT INTO smem2_ascii (ascii_num, ascii_chr) VALUES (90,'Z')

