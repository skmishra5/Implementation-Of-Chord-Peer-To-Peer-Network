# Implementation-Of-Chord-Peer-To-Peer-Network
The objective of the project is to build a simple peer to peer network where individual peers have
16-bit identifiers. This project has several sub-items associated with it: this relates to constructing
the logical overlay and traversing the network efficiently to store and retrieve content.

**Generating identifiers**

local timestamps are used to generate the 16-bit identifier for the first time a peer is created. The total number of peers in the system
can be about 64,000. When a peer starts up for, this ID is allowed to be specified (as a HEX string) at the command line.

**The Discovery Node**

There will also be a discovery node in the system that maintains information about the list of peers in
the system. Every time a peer joins or exits the system it notifies this discovery node. The registration
information includes information about the peer such as:

  - Its 16-bit identifier
  
  - The {host:port} information (please use TCP for communications)
  
  - A nickname for this node
  
In the unlikely case that there is a collision in the ID space, the discovery node notifies the peer about
this collision at which point the peer will regenerate a new identifier and repeat the process.
The discovery node has been introduced here to simplify the process of discovering the first peer that
will be the entry point into the system. The discovery node is ONLY responsible only for

  1. Returning ONE random node from the set of registered nodes
  
  2. Detect collisions
  
**Finger Table:**

In the Chord system with a 16-bit ID space; each peer maintains a Finger Table (FT) with 16 entries.
This FT is used to traverse the overlay efficiently. The ith entry in the FT of a peer corresponds to a
successor peer that is 2i-1 hops away. The ith entry in the FT of a peer with id p is FTp[i] = succ(p + 2i1
). In a case where all 216 peers are present; the FT allows you to reach peers that are 1, 2, 4, …, 215
hops away.

A data item with a computed digest of k is stored at a peer with the smallest identifier that is >= k.
This is the successor of k, and is represented as succ(k). The nodes are organized in a ring, so it is
possible that a node’s successor has a value that is less-than the identifier of the peer in question.

**Storing data items**

FT is used to store data items at the appropriate node. A data item with a key k will be
stored at the peer with the smallest identifier, p, such that p >= k. The data item that will be given to
you will be images. To support this feature you will develop a StoreData program that accepts as input
the file that needs to be stored. This StoreData program contacts a random peer (you can contact the
Discovery node to retrieve this information). The StoreData program will first compute the 16-bit
digest for the image and then use this hash to lookup the peer where it should be stored: you will be
contacting the aforementioned random peer to initiate this lookup; the node that gets back to you will
be the node that is most suitable to store your data. The file is then transferred to that suitable peer,
which is responsible for storing the file in the /tmp directory of the machine that it is running on.

**Addition of a node:**

Each node also keeps track of its predecessor. When a node finds a successor node, it informs this
node that it is now its predecessor. To maintain correctness, at regular intervals (this should be
configurable so that we can test this feature during the scoring process) each node q uses the first
entry in its FT to contact succ(q+1) and requests it to return its predecessor. This should be q: if this
is not the case, we know that a new node p has entered the system q < p <= succ(q+1) in which case
q has to reset its successor to p. It will then check to see if p has recorded q as its predecessor; if not,
another adjustment of FTq[1] will need to be made.

The addition of a new node impacts the overlay network in two ways. First, this results in updates to
the FT at one or more peers. Second, the addition of a new node should result in migration of data
items from peers that were originally holding them. 


